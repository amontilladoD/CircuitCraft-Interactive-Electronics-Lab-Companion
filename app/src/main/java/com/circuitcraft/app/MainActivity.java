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
import com.circuitcraft.app.receiver.SystemEventReceiver;
import com.circuitcraft.app.ui.AppSharer;
import com.circuitcraft.app.ui.BaseActivity;
import com.circuitcraft.app.ui.CalculatorActivity;
import com.circuitcraft.app.ui.ComponentsActivity;
import com.circuitcraft.app.ui.FavouritesActivity;
import com.circuitcraft.app.ui.LabActivity;
import com.circuitcraft.app.ui.LeaderboardActivity;
import com.circuitcraft.app.ui.LoginActivity;
import com.circuitcraft.app.ui.QuizActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private NetworkReceiver networkReceiver;
    private SystemEventReceiver systemReceiver;
    private boolean batteryLow, airplane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {                      // safety: not signed in
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String who = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                ? user.getDisplayName() : user.getEmail();
        binding.tvWelcome.setText("Hello, " + who + " 👋");

        // ---- Explicit intents: open our own screens ----
        binding.btnComponents.setOnClickListener(v -> open(ComponentsActivity.class));
        binding.btnCalculator.setOnClickListener(v -> open(CalculatorActivity.class));
        binding.btnLab.setOnClickListener(v -> open(LabActivity.class));
        binding.btnQuiz.setOnClickListener(v -> open(QuizActivity.class));
        binding.btnLeaderboard.setOnClickListener(v -> open(LeaderboardActivity.class));
        binding.btnFavourites.setOnClickListener(v -> open(FavouritesActivity.class));

        // ---- Implicit intent 1: open a web page (any browser app can handle it) ----
        binding.btnHelp.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.allaboutcircuits.com/textbook/"))));

        // ---- Implicit intent 2: share the APK file + GitHub link + a short message ----
        binding.btnShare.setOnClickListener(v -> AppSharer.share(this));

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

        // 1) connectivity
        networkReceiver = new NetworkReceiver(online ->
                binding.tvOffline.setVisibility(online ? View.GONE : View.VISIBLE));
        registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        binding.tvOffline.setVisibility(NetworkReceiver.isOnline(this) ? View.GONE : View.VISIBLE);

        // 2) low battery + airplane mode
        batteryLow = SystemEventReceiver.isBatteryLow(this);
        airplane = SystemEventReceiver.isAirplaneOn(this);
        updateAlert();
        systemReceiver = new SystemEventReceiver(new SystemEventReceiver.Listener() {
            @Override public void onBatteryLow() { batteryLow = true; updateAlert(); }
            @Override public void onBatteryOkay() { batteryLow = false; updateAlert(); }
            @Override public void onAirplaneMode(boolean on) { airplane = on; updateAlert(); }
        });
        registerReceiver(systemReceiver, SystemEventReceiver.filter());
    }

    private void updateAlert() {
        StringBuilder sb = new StringBuilder();
        if (batteryLow) sb.append("🔋 Battery is low - the simulator pauses to save power");
        if (airplane) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("✈ Airplane mode is ON - cloud features (sign-in, leaderboard) need internet");
        }
        binding.tvAlert.setText(sb.toString());
        binding.tvAlert.setVisibility(sb.length() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);   // always unregister to avoid leaks
            networkReceiver = null;
        }
        if (systemReceiver != null) {
            unregisterReceiver(systemReceiver);
            systemReceiver = null;
        }
    }

    @Override protected void onResume()  { super.onResume();  Log.d(TAG, "onResume"); }
    @Override protected void onPause()   { super.onPause();   Log.d(TAG, "onPause"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
