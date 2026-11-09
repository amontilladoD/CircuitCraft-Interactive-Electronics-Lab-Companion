package com.circuitcraft.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.databinding.ActivitySplashBinding;

/** First screen: logo for 2 seconds, then Login (Login skips to the dashboard if already signed in). */
public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_MS = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handler.postDelayed(() -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }, SPLASH_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);   // cancel if rotated / closed early
        super.onDestroy();
    }
}
