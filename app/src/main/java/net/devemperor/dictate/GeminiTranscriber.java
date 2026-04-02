package net.devemperor.dictate;

import android.util.Base64;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class GeminiTranscriber {

    private static final String TAG = "GeminiTranscriber";

    private final String apiKey;

    public GeminiTranscriber(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Transcribe an audio file using Gemini's multimodal API.
     *
     * @param audioFile       the M4A/AAC file to transcribe
     * @param language        BCP-47 language code (e.g. "si" for Sinhala), or null/empty for auto-detect
     * @param punctuationPrompt style hint prompt
     * @param professionContext profession context prefix (may be empty)
     * @return the transcribed text
     * @throws RateLimitException if Gemini rate-limit is hit
     * @throws RuntimeException   on other errors
     */
    public String transcribe(File audioFile, String language, String punctuationPrompt, String professionContext) {
        try {
            byte[] audioBytes = readFileBytes(audioFile);
            String base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP);

            StringBuilder promptBuilder = new StringBuilder();
            if (professionContext != null && !professionContext.isEmpty()) {
                promptBuilder.append(professionContext).append("\n\n");
            }
            promptBuilder.append("Transcribe this audio");
            if (language != null && !language.isEmpty() && !language.equals("detect")) {
                promptBuilder.append(" in language code '").append(language).append("'");
            }
            if (punctuationPrompt != null && !punctuationPrompt.isEmpty()) {
                promptBuilder.append(". ").append(punctuationPrompt);
            }
            promptBuilder.append(". Output only the transcribed text, nothing else.");

            GenerativeModel model = new GenerativeModel("gemini-2.0-flash", apiKey);
            GenerativeModelFutures futures = GenerativeModelFutures.from(model);

            Content content = new Content.Builder()
                    .addText(promptBuilder.toString())
                    .addBlob("audio/mp4", audioBytes)
                    .build();

            GenerateContentResponse response = futures.generateContent(content).get();
            String text = response.getText();
            return text != null ? text.trim() : "";

        } catch (ExecutionException e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (msg != null && (msg.contains("429") || msg.toLowerCase().contains("rate") || msg.toLowerCase().contains("quota"))) {
                throw new RateLimitException("Gemini rate limit hit: " + msg);
            }
            Log.e(TAG, "Gemini transcription failed", e);
            throw new RuntimeException("Gemini transcription failed: " + msg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gemini transcription interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read audio file", e);
        }
    }

    /**
     * Reword / post-process text using Gemini.
     *
     * @param prompt the full prompt (instruction + text)
     * @param model  Gemini model name to use
     * @return the generated output text
     * @throws RateLimitException if rate-limit is hit
     */
    public String reword(String prompt, String model) {
        try {
            GenerativeModel generativeModel = new GenerativeModel(model, apiKey);
            GenerativeModelFutures futures = GenerativeModelFutures.from(generativeModel);

            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();

            GenerateContentResponse response = futures.generateContent(content).get();
            String text = response.getText();
            return text != null ? text.trim() : "";

        } catch (ExecutionException e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            if (msg != null && (msg.contains("429") || msg.toLowerCase().contains("rate") || msg.toLowerCase().contains("quota"))) {
                throw new RateLimitException("Gemini rate limit hit: " + msg);
            }
            Log.e(TAG, "Gemini reword failed", e);
            throw new RuntimeException("Gemini reword failed: " + msg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gemini reword interrupted", e);
        }
    }

    private byte[] readFileBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = 0;
            while (bytesRead < bytes.length) {
                int n = fis.read(bytes, bytesRead, bytes.length - bytesRead);
                if (n == -1) break;
                bytesRead += n;
            }
        }
        return bytes;
    }

    /** Thrown when Gemini hits its rate limit so the caller can fall back. */
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
