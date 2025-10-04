package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SecurityActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private AppCompatButton btnChangePassword;
    private TextView btnLogoutAllDevices, btnDeactivate, btnDeleteAccount;
    private LinearLayout containerDevices;

    private FirebaseUser user;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogoutAllDevices = findViewById(R.id.btnLogoutAllDevices);
        btnDeactivate = findViewById(R.id.btnDeactivate);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        containerDevices = findViewById(R.id.containerDevices);

        TextView tvBack = findViewById(R.id.tvBack);
        tvBack.setOnClickListener(v -> {
            // Simply finish the current activity to go back
            finish();
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (user == null) {
            android.util.Log.e("SECURITY", "User is null in SecurityActivity!");
        } else {
            android.util.Log.d("SECURITY", "User logged in: " + user.getEmail());
            saveCurrentDevice(); // ✅ Save this device into Firestore
        }

        // ✅ Change Password logic
        btnChangePassword.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Enter current password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.isEmpty() || newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (user != null && user.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> {
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show();

                                        // Log password change
                                        Map<String, Object> log = new HashMap<>();
                                        log.put("username", user.getEmail());
                                        log.put("datestamp", System.currentTimeMillis());
                                        log.put("reason", "user changed password");

                                        db.collection("logs").add(log);
                                        android.util.Log.d("SECURITY", "Password change logged in Firestore.");
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("SECURITY", "Update password failed", e);
                                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("SECURITY", "Re-authentication failed", e);
                            Toast.makeText(this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        // ✅ Load devices on screen with Logcat debug
        loadDevices();

        // ✅ Logout all devices except this one
        btnLogoutAllDevices.setOnClickListener(v -> logoutAllDevices());

        btnDeactivate.setOnClickListener(v -> {
            if (user == null) return;

            db.collection("users")
                    .document(user.getUid())
                    .update("status", "deactivated")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Account deactivated (temporary)", Toast.LENGTH_SHORT).show();
                        android.util.Log.d("SECURITY", "Account deactivated for UID: " + user.getUid());

                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(SecurityActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to deactivate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        android.util.Log.e("SECURITY", "Deactivate failed", e);
                    });
        });

        btnDeleteAccount.setOnClickListener(v -> {
            if (user == null) return;

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        user.delete()
                                .addOnSuccessListener(aVoid -> {
                                    db.collection("users").document(user.getUid()).delete();
                                    Toast.makeText(this, "Account deleted permanently", Toast.LENGTH_SHORT).show();
                                    android.util.Log.d("SECURITY", "Account deleted UID: " + user.getUid());
                                    FirebaseAuth.getInstance().signOut();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    android.util.Log.e("SECURITY", "Delete account failed", e);
                                });
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    // ✅ Save current device in Firestore
    private void saveCurrentDevice() {
        if (user == null) return;

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String model = android.os.Build.MODEL;
        String os = "Android " + android.os.Build.VERSION.RELEASE;

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("model", model);
        deviceInfo.put("os", os);
        deviceInfo.put("lastLogin", System.currentTimeMillis());

        db.collection("users")
                .document(user.getUid())
                .collection("devices")
                .document(deviceId) // use deviceId as doc id
                .set(deviceInfo)
                .addOnSuccessListener(aVoid -> android.util.Log.d("SECURITY", "Current device saved: " + model))
                .addOnFailureListener(e -> android.util.Log.e("SECURITY", "Failed to save current device", e));
    }

    private void loadDevices() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            android.util.Log.d("SECURITY", "Fetching devices for UID: " + user.getUid());
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .collection("devices")
                    .get()
                    .addOnSuccessListener(query -> {
                        containerDevices.removeAllViews();
                        android.util.Log.d("SECURITY", "Devices found: " + query.size());

                        if (query.isEmpty()) {
                            android.util.Log.w("SECURITY", "No devices in Firestore for this user.");
                            TextView tv = new TextView(this);
                            tv.setText("(No devices found)");
                            tv.setTextSize(12);
                            containerDevices.addView(tv);
                        } else {
                            for (var doc : query.getDocuments()) {
                                String model = doc.getString("model");
                                String os = doc.getString("os");
                                Long lastLogin = doc.getLong("lastLogin");

                                android.util.Log.d("SECURITY", "Loaded device -> Model: " + model + ", OS: " + os);

                                // ✅ Create container for each device
                                LinearLayout deviceContainer = new LinearLayout(this);
                                deviceContainer.setOrientation(LinearLayout.VERTICAL);
                                deviceContainer.setBackgroundResource(R.drawable.dialog_background);

                                // ✅ Device info text
                                TextView tv = new TextView(this);
                                tv.setText(model + " | " + os + " | Last login: " +
                                        DateFormat.format("yyyy-MM-dd HH:mm", lastLogin));
                                tv.setTextSize(14);

                                // Add TextView into device container
                                deviceContainer.addView(tv);

                                // Add device container into main container
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 15, 0, 15); // spacing between devices
                                containerDevices.addView(deviceContainer, params);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("SECURITY", "Failed to load devices", e);
                        Toast.makeText(this, "Error loading devices", Toast.LENGTH_SHORT).show();
                    });
        } else {
            android.util.Log.e("SECURITY", "User is null!");
        }
    }

    private void logoutAllDevices() {
        if (user == null) return;

        String currentDeviceId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        db.collection("users")
                .document(user.getUid())
                .collection("devices")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query.getDocuments()) {
                        String deviceId = doc.getString("deviceId");
                        if (deviceId != null && !deviceId.equals(currentDeviceId)) {
                            doc.getReference().delete();
                            android.util.Log.d("SECURITY", "Deleted device: " + deviceId);
                        }
                    }
                    Toast.makeText(this, "Logged out of other devices!", Toast.LENGTH_SHORT).show();
                    loadDevices(); // refresh UI
                })
                .addOnFailureListener(e -> android.util.Log.e("SECURITY", "Failed logoutAllDevices", e));
    }

}
