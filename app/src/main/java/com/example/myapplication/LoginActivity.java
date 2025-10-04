package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnSignup;
    private FirebaseAuth mAuth;
    private ListenerRegistration deviceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // user already logged in → save device info
            saveCurrentDevice(currentUser);
            startDeviceWatcher(currentUser);
            navigateToMainActivity();
        }
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            // ✅ Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                // 🔍 Check status in Firestore
                                db.collection("users")
                                        .document(user.getUid())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String status = documentSnapshot.getString("status");

                                                if ("deactivated".equalsIgnoreCase(status)) {
                                                    // ✅ Reactivate account
                                                    db.collection("users")
                                                            .document(user.getUid())
                                                            .update("status", "active")
                                                            .addOnSuccessListener(aVoid -> {
                                                                android.util.Log.d("SECURITY", "Account reactivated for UID: " + user.getUid());
                                                                Toast.makeText(LoginActivity.this, "Welcome back! Your account has been reactivated.", Toast.LENGTH_SHORT).show();

                                                                // Save device info + continue
                                                                saveCurrentDevice(user);
                                                                navigateToMainActivity();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                android.util.Log.e("SECURITY", "Failed to reactivate", e);
                                                                Toast.makeText(LoginActivity.this, "Login failed: Could not reactivate account.", Toast.LENGTH_SHORT).show();
                                                            });
                                                } else {
                                                    // ✅ Already active → just continue
                                                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                                    saveCurrentDevice(user);
                                                    navigateToMainActivity();
                                                }
                                            } else {
                                                Toast.makeText(LoginActivity.this, "User record not found.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("SECURITY", "Failed to fetch user status", e);
                                            Toast.makeText(LoginActivity.this, "Login failed: Could not fetch account status.", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                        else {
                            // ❌ Sign in failed
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() : "Login failed";

                            if (errorMessage.contains("no user record")) {
                                Toast.makeText(LoginActivity.this, "No account found with this email. Please sign up first.", Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("password is invalid")) {
                                Toast.makeText(LoginActivity.this, "Invalid password. Please try again.", Toast.LENGTH_LONG).show();
                            } else if (errorMessage.contains("email address is badly formatted")) {
                                Toast.makeText(LoginActivity.this, "Invalid email format.", Toast.LENGTH_SHORT).show();
                            } else if (errorMessage.contains("network")) {
                                Toast.makeText(LoginActivity.this, "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    // ✅ Save current device in Firestore
    private void saveCurrentDevice(FirebaseUser user) {
        String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        String model = android.os.Build.MODEL;
        String os = "Android " + android.os.Build.VERSION.RELEASE;

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("model", model);
        deviceInfo.put("os", os);
        deviceInfo.put("lastLogin", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("devices")
                .document(deviceId) // ✅ unique per device
                .set(deviceInfo)
                .addOnSuccessListener(aVoid ->
                        android.util.Log.d("SECURITY", "Device saved: " + model))
                .addOnFailureListener(e ->
                        android.util.Log.e("SECURITY", "Failed to save device", e));
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, TabbedActivity.class);
        startActivity(intent);
        finish();
    }

    private void startDeviceWatcher(FirebaseUser user) {
        String deviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        deviceListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("devices")
                .document(deviceId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("SECURITY", "Device watcher failed", e);
                        return;
                    }
                    if (snapshot != null && !snapshot.exists()) {
                        // Device deleted remotely → logout
                        FirebaseAuth.getInstance().signOut();
                        android.util.Log.d("SECURITY", "This device was logged out remotely!");

                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceListener != null) deviceListener.remove();
    }
}
