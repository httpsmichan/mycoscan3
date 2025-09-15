package com.example.myapplication;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String currentUserEmail;
    private FirebaseUser currentUser;

    private LinearLayout container; // parent layout to hold rows

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 40, 40, 40);
        setContentView(container);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser.getUid();
        currentUserEmail = currentUser != null ? currentUser.getEmail() : "anonymous";

        // Load existing data
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                addEditableRow("username", document.getString("username"));
                addEditableRow("fullName", document.getString("fullName"));
                addReadOnlyRow("email", document.getString("email")); // 🔹 email is display only
                addEditableRow("address", document.getString("address"));
                addEditableRow("mobile", document.getString("mobile"));
                addEditableRow("social", document.getString("social"));
            }
        });
    }

    /**
     * Editable row (text + button)
     */
    private void addEditableRow(String fieldName, String currentValue) {
        EditableRow row = new EditableRow(fieldName, currentValue);
        container.addView(row);
    }

    /**
     * Read-only row (text only, no button)
     */
    private void addReadOnlyRow(String fieldName, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        EditText editText = new EditText(this);
        editText.setText(value != null ? value : "");
        editText.setEnabled(false);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));
        row.addView(editText);

        container.addView(row);
    }

    /**
     * Inner class representing one editable field row
     */
    private class EditableRow extends LinearLayout {
        private EditText editText;
        private Button btnToggle;
        private boolean isEditing = false;

        public EditableRow(String fieldName, String value) {
            super(EditProfileActivity.this);
            setOrientation(HORIZONTAL);

            // EditText
            editText = new EditText(EditProfileActivity.this);
            editText.setText(value != null ? value : "");
            editText.setEnabled(false);
            editText.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            addView(editText);

            // Button
            btnToggle = new Button(EditProfileActivity.this);
            btnToggle.setText("Edit");
            addView(btnToggle);

            // Toggle logic
            btnToggle.setOnClickListener(v -> {
                if (!isEditing) {
                    editText.setEnabled(true);
                    editText.requestFocus();
                    btnToggle.setText("Save");
                    isEditing = true;
                } else {
                    String newValue = editText.getText().toString().trim();
                    if (newValue.isEmpty()) {
                        Toast.makeText(EditProfileActivity.this,
                                "Field cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Firestore update for normal fields
                    Map<String, Object> update = new HashMap<>();
                    update.put(fieldName, newValue);

                    db.collection("users").document(userId)
                            .update(update)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(EditProfileActivity.this,
                                        fieldName + " updated!", Toast.LENGTH_SHORT).show();

                                logChange(fieldName, newValue);

                                editText.setEnabled(false);
                                btnToggle.setText("Edit");
                                isEditing = false;
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(EditProfileActivity.this,
                                            "Failed to update " + fieldName,
                                            Toast.LENGTH_SHORT).show());
                }
            });
        }

        /**
         * Log changes to Firestore logs
         */
        private void logChange(String fieldName, String newValue) {
            Map<String, Object> log = new HashMap<>();
            log.put("username", currentUserEmail);
            log.put("datestamp", System.currentTimeMillis());
            log.put("change", fieldName + " updated to: " + newValue);

            FirebaseFirestore.getInstance()
                    .collection("logs")
                    .add(log);
        }
    }
}
