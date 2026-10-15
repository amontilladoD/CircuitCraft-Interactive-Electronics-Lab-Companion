package com.circuitcraft.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.circuitcraft.app.data.FavouriteRepository;
import com.circuitcraft.app.databinding.ActivityFavouritesBinding;
import com.circuitcraft.app.model.Favourite;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class FavouritesActivity extends AppCompatActivity {
    private ActivityFavouritesBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityFavouritesBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.rvFavourites.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadList();      
    }

    private void loadList() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        List<Favourite> list = FavouriteRepository.loadAll(this, user.getUid());
        b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        b.rvFavourites.setAdapter(new FavouriteAdapter(list, new FavouriteAdapter.Listener() {
            @Override
            public void onOpen(Favourite f) {
                Intent i = new Intent(FavouritesActivity.this, LabActivity.class);
                i.putExtra(LabActivity.EXTRA_JSON, f.data);   
                startActivity(i);
            }

            @Override
            public void onDelete(Favourite f) {
                new AlertDialog.Builder(FavouritesActivity.this)
                        .setTitle("Delete \"" + f.name + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            FavouriteRepository.delete(FavouritesActivity.this, f);
                            loadList();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }));
    }
}
