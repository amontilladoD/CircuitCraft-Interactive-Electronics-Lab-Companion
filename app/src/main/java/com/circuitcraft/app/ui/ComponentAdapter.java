package com.circuitcraft.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.circuitcraft.app.databinding.ItemComponentBinding;
import com.circuitcraft.app.model.ComponentItem;

import java.util.List;

/** RecyclerView Adapter: turns a List<ComponentItem> into item_component rows. */
public class ComponentAdapter extends RecyclerView.Adapter<ComponentAdapter.VH> {

    public interface OnItemClick {
        void onClick(ComponentItem item);
    }

    private final List<ComponentItem> items;
    private final OnItemClick listener;

    public ComponentAdapter(List<ComponentItem> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {   // 1) create a row view
        ItemComponentBinding b = ItemComponentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {          // 2) fill it with data
        ComponentItem item = items.get(position);
        holder.b.tvIcon.setText(item.icon);
        holder.b.tvName.setText(item.name);
        holder.b.tvCategory.setText(item.category);
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {                                                // 3) how many rows
        return items.size();
    }

    /** ViewHolder keeps references to the row's views so findViewById isn't repeated. */
    static class VH extends RecyclerView.ViewHolder {
        final ItemComponentBinding b;

        VH(ItemComponentBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
