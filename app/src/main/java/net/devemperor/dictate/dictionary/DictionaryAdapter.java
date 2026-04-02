package net.devemperor.dictate.dictionary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.devemperor.dictate.R;

import java.util.List;

public class DictionaryAdapter extends RecyclerView.Adapter<DictionaryAdapter.ViewHolder> {

    public interface Callback {
        void onItemClick(CustomWordEntity item, int position);
        void onItemLongClick(CustomWordEntity item, int position);
    }

    private final List<CustomWordEntity> items;
    private final Callback callback;

    public DictionaryAdapter(List<CustomWordEntity> items, Callback callback) {
        this.items = items;
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dictionary_word, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomWordEntity item = items.get(position);
        holder.triggerTv.setText(item.triggerWord);
        holder.replacementTv.setText("→ " + item.replacement);
        holder.itemView.setOnClickListener(v -> callback.onItemClick(item, holder.getAdapterPosition()));
        holder.itemView.setOnLongClickListener(v -> {
            callback.onItemLongClick(item, holder.getAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView triggerTv;
        TextView replacementTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            triggerTv = itemView.findViewById(R.id.item_dict_trigger_tv);
            replacementTv = itemView.findViewById(R.id.item_dict_replacement_tv);
        }
    }
}
