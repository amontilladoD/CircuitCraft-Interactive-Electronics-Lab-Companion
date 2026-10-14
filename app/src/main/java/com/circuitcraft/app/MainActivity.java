package com.circuitcraft.app;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.databinding.ActivityMainBinding;
import com.circuitcraft.app.receiver.NetworkReceiver;
import com.circuitcraft.app.ui.CalculatorActivity;
import com.circuitcraft.app.ui.ComponentsActivity;
import com.circuitcraft.app.ui.FavouritesActivity;
import com.circuitcraft.app.ui.LabActivity;
import com.circuitcraft.app.ui.LoginActivity;
import com.circuitcraft.app.ui.QuizActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";   
    private ActivityMainBinding binding;
    private NetworkReceiver networkReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {                      
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String who = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : user.getEmail();
        binding.tvWelcome.setText("Hello, " + who + " 👋");

        binding.btnComponents.setOnClickListener(v -> open(ComponentsActivity.class));
        binding.btnCalculator.setOnClickListener(v -> open(CalculatorActivity.class));
        binding.btnLab.setOnClickListener(v -> open(LabActivity.class));
        binding.btnQuiz.setOnClickListener(v -> open(QuizActivity.class));
        binding.btnFavourites.setOnClickListener(v -> open(FavouritesActivity.class));

        binding.btnHelp.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.allaboutcircuits.com/textbook/"))));

        binding.btnShare.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, "Learn electronics with CircuitCraft - interactive lab companion!");
            startActivity(Intent.createChooser(send, "Share via"));
        });

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void open(Class<?> target) {
        startActivity(new Intent(this, target));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        
        networkReceiver = new NetworkReceiver(online ->
                binding.tvOffline.setVisibility(online ? View.GONE : View.VISIBLE));
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        binding.tvOffline.setVisibility(NetworkReceiver.isOnline(this) ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);   
            networkReceiver = null;
        }
    }

    @Override protected void onResume()  { super.onResume();  Log.d(TAG, "onResume"); }
    @Override protected void onPause()   { super.onPause();   Log.d(TAG, "onPause"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
