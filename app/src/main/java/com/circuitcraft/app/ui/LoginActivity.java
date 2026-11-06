package com.circuitcraft.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.MainActivity;
import com.circuitcraft.app.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

/** Sign-in screen (Firebase Authentication, email + password). */
public class LoginActivity extends BaseActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());   // View Binding
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();

        // Already signed in? skip this screen.
        if (auth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> login());
        // EXPLICIT intent: we name the exact Activity to open
        binding.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String email = binding.etEmail.getText().toString().trim();
        String pass = binding.etPassword.getText().toString();

        if (email.isEmpty()) { binding.etEmail.setError("Enter your email"); return; }
        if (pass.isEmpty()) { binding.etPassword.setError("Enter your password"); return; }

        binding.progress.setVisibility(View.VISIBLE);
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            binding.progress.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                goToMain();
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Sign in failed: " + msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear back stack
        startActivity(i);
        finish();
    }
}
