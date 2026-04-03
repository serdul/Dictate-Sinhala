package net.devemperor.dictate.dictionary;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import net.devemperor.dictate.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DictionaryActivity extends AppCompatActivity {

    private DictionaryRepository repository;
    private List<CustomWordEntity> items;
    private DictionaryAdapter adapter;
    private TextView emptyTv;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.wani_dictionary_activity_title);
        }

        executor = Executors.newSingleThreadExecutor();
        repository = new DictionaryRepository(this);
        emptyTv = findViewById(R.id.dictionary_empty_tv);

        RecyclerView recyclerView = findViewById(R.id.dictionary_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadItems(recyclerView);

        FloatingActionButton fab = findViewById(R.id.dictionary_fab);
        fab.setOnClickListener(v -> showEditDialog(null, -1, recyclerView));
    }

    private void loadItems(RecyclerView recyclerView) {
        executor.execute(() -> {
            items = repository.getAll();
            runOnUiThread(() -> {
                adapter = new DictionaryAdapter(items, new DictionaryAdapter.Callback() {
                    @Override
                    public void onItemClick(CustomWordEntity item, int position) {
                        showEditDialog(item, position, recyclerView);
                    }

                    @Override
                    public void onItemLongClick(CustomWordEntity item, int position) {
                        new MaterialAlertDialogBuilder(DictionaryActivity.this)
                                .setTitle(R.string.wani_dictionary_delete_confirm_title)
                                .setMessage(R.string.wani_dictionary_delete_confirm_message)
                                .setPositiveButton(R.string.dictate_yes, (d, w) -> deleteItem(item, position))
                                .setNegativeButton(R.string.dictate_no, null)
                                .show();
                    }
                });
                recyclerView.setAdapter(adapter);
                updateEmptyState();

                new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) { return false; }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int pos = viewHolder.getAdapterPosition();
                        CustomWordEntity item = items.get(pos);
                        new MaterialAlertDialogBuilder(DictionaryActivity.this)
                                .setTitle(R.string.wani_dictionary_delete_confirm_title)
                                .setMessage(R.string.wani_dictionary_delete_confirm_message)
                                .setPositiveButton(R.string.dictate_yes, (d, w) -> deleteItem(item, pos))
                                .setNegativeButton(R.string.dictate_no, (d, w) -> adapter.notifyItemChanged(pos))
                                .show();
                    }
                }).attachToRecyclerView(recyclerView);
            });
        });
    }

    private void showEditDialog(CustomWordEntity existing, int position, RecyclerView recyclerView) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_dictionary_word, null);
        TextInputEditText triggerEt = dialogView.findViewById(R.id.dict_dialog_trigger_et);
        TextInputEditText replacementEt = dialogView.findViewById(R.id.dict_dialog_replacement_et);

        if (existing != null) {
            triggerEt.setText(existing.triggerWord);
            replacementEt.setText(existing.replacement);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? R.string.wani_dictionary_add_word : R.string.wani_dictionary_save)
                .setView(dialogView)
                .setPositiveButton(R.string.wani_dictionary_save, (d, w) -> {
                    String trigger = triggerEt.getText() != null ? triggerEt.getText().toString().trim() : "";
                    String replacement = replacementEt.getText() != null ? replacementEt.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(trigger)) {
                        Toast.makeText(this, R.string.wani_dictionary_trigger_word_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (existing == null) {
                        CustomWordEntity newWord = new CustomWordEntity(trigger, replacement, null);
                        executor.execute(() -> {
                            repository.insert(newWord);
                            runOnUiThread(() -> {
                                items.add(newWord);
                                adapter.notifyItemInserted(items.size() - 1);
                                updateEmptyState();
                            });
                        });
                    } else {
                        existing.triggerWord = trigger;
                        existing.replacement = replacement;
                        executor.execute(() -> {
                            repository.update(existing);
                            runOnUiThread(() -> adapter.notifyItemChanged(position));
                        });
                    }
                })
                .setNegativeButton(R.string.dictate_cancel, null)
                .show();
    }

    private void deleteItem(CustomWordEntity item, int position) {
        executor.execute(() -> {
            repository.delete(item);
            runOnUiThread(() -> {
                if (position >= 0 && position < items.size()) {
                    items.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyState();
                }
            });
        });
    }

    private void updateEmptyState() {
        emptyTv.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdown();
    }
}
