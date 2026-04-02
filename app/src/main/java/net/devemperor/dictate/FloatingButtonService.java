package net.devemperor.dictate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.devemperor.dictate.GeminiTranscriber;
import net.devemperor.dictate.R;
import net.devemperor.dictate.settings.DictateSettingsActivity;

import java.io.File;
import java.io.IOException;

public class FloatingButtonService extends Service {

    private static final String TAG = "FloatingButtonService";
    private static final String CHANNEL_ID = "wani_floating_mic";
    private static final int NOTIF_ID = 1001;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private SharedPreferences sp;
    private Handler mainHandler;

    private MediaRecorder recorder;
    private File audioFile;
    private boolean isRecording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        sp = getSharedPreferences("net.devemperor.dictate", MODE_PRIVATE);

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        setupFloatingView();
    }

    private void setupFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int savedX = sp.getInt("net.devemperor.dictate.floating_btn_x", 100);
        int savedY = sp.getInt("net.devemperor.dictate.floating_btn_y", 300);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = savedX;
        params.y = savedY;

        LayoutInflater inflater = LayoutInflater.from(new ContextThemeWrapper(this, R.style.Theme_Dictate));
        floatingView = inflater.inflate(R.layout.layout_floating_button, null);

        FloatingActionButton fab = floatingView.findViewById(R.id.floating_mic_fab);

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;
            private boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - initialTouchX);
                        int dy = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        sp.edit()
                                .putInt("net.devemperor.dictate.floating_btn_x", params.x)
                                .putInt("net.devemperor.dictate.floating_btn_y", params.y)
                                .apply();
                        if (!moved) {
                            long duration = System.currentTimeMillis() - touchStartTime;
                            if (duration < 500) {
                                onFabTap();
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add floating view", e);
        }
    }

    private void onFabTap() {
        if (!isRecording) {
            startFloatingRecording();
        } else {
            stopFloatingRecording();
        }
    }

    private void startFloatingRecording() {
        try {
            audioFile = new File(getCacheDir(), "floating_audio.m4a");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(64000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(audioFile);
            recorder.prepare();
            recorder.start();
            isRecording = true;
            mainHandler.post(() -> {
                FloatingActionButton fab = floatingView.findViewById(R.id.floating_mic_fab);
                if (fab != null) fab.setImageResource(R.drawable.ic_baseline_mic_24);
                Toast.makeText(FloatingButtonService.this, "Recording…", Toast.LENGTH_SHORT).show();
            });
        } catch (IOException e) {
            Log.e(TAG, "Floating recording failed", e);
        }
    }

    private void stopFloatingRecording() {
        if (recorder != null) {
            try { recorder.stop(); } catch (RuntimeException ignored) {}
            recorder.release();
            recorder = null;
        }
        isRecording = false;
        mainHandler.post(() -> Toast.makeText(this, "Processing…", Toast.LENGTH_SHORT).show());
        transcribeAndOutput();
    }

    private void transcribeAndOutput() {
        int transcriptionProvider = sp.getInt("net.devemperor.dictate.transcription_provider", 0);
        String[] providerValues = getResources().getStringArray(R.array.dictate_api_providers_values);
        String apiHost = (transcriptionProvider < providerValues.length) ? providerValues[transcriptionProvider] : "";

        // Read the user's current input language (first selected language)
        java.util.Set<String> inputLanguages = sp.getStringSet("net.devemperor.dictate.input_languages", null);
        String language = "si"; // default to Sinhala
        if (inputLanguages != null && !inputLanguages.isEmpty()) {
            language = inputLanguages.iterator().next();
        }
        final String finalLanguage = language;

        if ("gemini".equals(apiHost)) {
            String geminiKey = sp.getString("net.devemperor.dictate.transcription_api_key_gemini", sp.getString("net.devemperor.dictate.transcription_api_key", ""));
            if (geminiKey.isEmpty()) {
                mainHandler.post(() -> Toast.makeText(this, "Please configure Gemini API key in settings", Toast.LENGTH_LONG).show());
                return;
            }
            String geminiModel = sp.getString("net.devemperor.dictate.transcription_gemini_model", "gemini-2.0-flash");
            new Thread(() -> {
                try {
                    GeminiTranscriber transcriber = new GeminiTranscriber(geminiKey);
                    String text = transcriber.transcribe(audioFile, finalLanguage, "", "", geminiModel);
                    outputText(text);
                } catch (GeminiTranscriber.RateLimitException e) {
                    mainHandler.post(() -> Toast.makeText(this, R.string.wani_floating_button_rate_limit_toast, Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    Log.e(TAG, "Transcription error", e);
                    mainHandler.post(() -> Toast.makeText(this, "Transcription failed", Toast.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            // OpenAI / Groq / Custom path
            String apiKey = sp.getString("net.devemperor.dictate.transcription_api_key", sp.getString("net.devemperor.dictate.api_key", ""));
            if (apiKey.isEmpty()) {
                mainHandler.post(() -> Toast.makeText(this, "Please configure API key in settings", Toast.LENGTH_LONG).show());
                return;
            }
            String finalApiHost = "custom_server".equals(apiHost)
                    ? sp.getString("net.devemperor.dictate.transcription_custom_host", "")
                    : apiHost;
            String model;
            switch (transcriptionProvider) {
                case 1: model = sp.getString("net.devemperor.dictate.transcription_groq_model", "whisper-large-v3-turbo"); break;
                case 2: model = sp.getString("net.devemperor.dictate.transcription_custom_model", ""); break;
                default: model = sp.getString("net.devemperor.dictate.transcription_openai_model", "gpt-4o-mini-transcribe");
            }
            final String finalModel = model;
            final String finalKey = apiKey.replaceAll("[^ -~]", "");
            new Thread(() -> {
                try {
                    com.openai.client.okhttp.OpenAIOkHttpClient client = com.openai.client.okhttp.OpenAIOkHttpClient.builder()
                            .apiKey(finalKey)
                            .baseUrl(finalApiHost)
                            .timeout(java.time.Duration.ofSeconds(120))
                            .build();
                    com.openai.models.audio.transcriptions.TranscriptionCreateParams.Builder params =
                            com.openai.models.audio.transcriptions.TranscriptionCreateParams.builder()
                                    .file(audioFile.toPath())
                                    .model(finalModel)
                                    .responseFormat(com.openai.models.audio.AudioResponseFormat.JSON);
                    if (!finalLanguage.equals("detect")) params.language(finalLanguage);
                    String text = client.audio().transcriptions().create(params.build()).asTranscription().text().strip();
                    outputText(text);
                } catch (Exception e) {
                    Log.e(TAG, "Transcription error", e);
                    mainHandler.post(() -> Toast.makeText(this, "Transcription failed", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void outputText(String text) {
        mainHandler.post(() -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Wani transcription", text));
                Toast.makeText(this, R.string.wani_floating_button_copied_toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.wani_floating_button_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.wani_floating_button_notification_text));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent settingsIntent = new Intent(this, DictateSettingsActivity.class);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_mic_24)
                .setContentTitle(getString(R.string.wani_floating_button_notification_title))
                .setContentText(getString(R.string.wani_floating_button_notification_text))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        }
        if (recorder != null) {
            try { recorder.stop(); } catch (RuntimeException ignored) {}
            recorder.release();
            recorder = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
