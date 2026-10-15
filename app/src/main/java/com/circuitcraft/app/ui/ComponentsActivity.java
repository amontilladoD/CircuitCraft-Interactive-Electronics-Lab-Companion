package com.circuitcraft.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.circuitcraft.app.data.ComponentRepository;
import com.circuitcraft.app.databinding.ActivityComponentsBinding;

public class ComponentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityComponentsBinding binding = ActivityComponentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.rvComponents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvComponents.setAdapter(new ComponentAdapter(ComponentRepository.getAll(), item ->
                new AlertDialog.Builder(this)
                        .setTitle(item.icon + "  " + item.name)
                        .setMessage(item.description + "\n\n⚠ Common mistake:\n" + item.tip)
                        .setPositiveButton("Got it", null)
                        .show()));
    }
}
