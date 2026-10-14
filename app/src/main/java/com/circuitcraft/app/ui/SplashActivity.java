package com.circuitcraft.app.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        animateLogo(logo);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }, SPLASH_DELAY_MS);
    }

    private void animateLogo(View logo) {
        
        logo.setAlpha(0f);
        logo.setScaleX(0.3f);
        logo.setScaleY(0.3f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        fadeIn.setDuration(800);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.3f, 1f);
        scaleX.setDuration(1000);
        scaleX.setInterpolator(new OvershootInterpolator(1.5f));

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.3f, 1f);
        scaleY.setDuration(1000);
        scaleY.setInterpolator(new OvershootInterpolator(1.5f));

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, scaleX, scaleY);
        set.setStartDelay(200);
        set.start();
    }
}
