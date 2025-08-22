package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUsername, etFullName, etAddress, etMobile, etSocial;
    private Button btnSave;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etUsername = findViewById(R.id.etUsername);
        etFullName = findViewById(R.id.etFullName);
        etAddress = findViewById(R.id.etAddress);
        etMobile = findViewById(R.id.etMobile);
        etSocial = findViewById(R.id.etSocial);
        btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load existing data
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                etUsername.setText(document.getString("username"));
                etFullName.setText(document.getString("fullName"));
                etAddress.setText(document.getString("address"));
                etMobile.setText(document.getString("mobile"));
                etSocial.setText(document.getString("social"));
            }
        });

        // Save changes
        btnSave.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("username", etUsername.getText().toString().trim());
            updates.put("fullName", etFullName.getText().toString().trim());
            updates.put("address", etAddress.getText().toString().trim());
            updates.put("mobile", etMobile.getText().toString().trim());
            updates.put("social", etSocial.getText().toString().trim());

            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
        });
    }
}
