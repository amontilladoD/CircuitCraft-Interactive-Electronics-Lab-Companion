package com.circuitcraft.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Sign-up screen. Firebase automatically signs a new user in, so after the profile is saved
 * we SIGN OUT again and send the user to the Sign-in screen: a real sign-in is always required.
 */
public class RegisterActivity extends BaseActivity {
    private ActivityRegisterBinding binding;
    private boolean finished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String pass = binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText().toString();

        if (name.isEmpty()) { binding.etName.setError("Enter your name"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEmail.setError("Invalid email"); return; }
        if (pass.length() < 6) { binding.etPassword.setError("At least 6 characters"); return; }
        if (!pass.equals(confirm)) { binding.etConfirm.setError("Passwords do not match"); return; }

        binding.progress.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                binding.progress.setVisibility(View.GONE);
                binding.btnRegister.setEnabled(true);
                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Sign up failed: " + msg, Toast.LENGTH_LONG).show();
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) { goToLogin(); return; }

            // save the profile while the new account is still signed in
            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(name).build());
            Map<String, Object> profile = new HashMap<>();
            profile.put("name", name);
            profile.put("email", email);
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(profile)
                    .addOnCompleteListener(t -> goToLogin());
            // if Firestore is slow/offline, don't wait forever
            new Handler(Looper.getMainLooper()).postDelayed(this::goToLogin, 3000);
        });
    }

    /** Sign out and go to the Sign-in screen (runs once). */
    private void goToLogin() {
        if (finished) return;
        finished = true;
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
