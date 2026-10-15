package com.circuitcraft.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.circuitcraft.app.databinding.ItemFavouriteBinding;
import com.circuitcraft.app.model.Favourite;

import java.util.List;

public class FavouriteAdapter extends RecyclerView.Adapter<FavouriteAdapter.VH> {

    public interface Listener {
        void onOpen(Favourite f);
        void onDelete(Favourite f);
    }

    private final List<Favourite> items;
    private final Listener listener;

    public FavouriteAdapter(List<Favourite> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemFavouriteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Favourite f = items.get(position);
        h.b.tvName.setText(f.name);
        h.b.tvDetails.setText(f.summary);
        h.itemView.setOnClickListener(v -> listener.onOpen(f));
        h.b.btnDelete.setOnClickListener(v -> listener.onDelete(f));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemFavouriteBinding b;

        VH(ItemFavouriteBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
