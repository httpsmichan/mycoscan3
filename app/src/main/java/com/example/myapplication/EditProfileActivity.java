package com.example.myapplication;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String currentUserEmail;
    private FirebaseUser currentUser;

    private LinearLayout container; // parent layout
    private LinearLayout achievementsContainer; // container for achievements
    private EditText etBio; // bio field
    private Button btnAddAchievement;

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

                // 🔹 Add Bio
                addBioField(document.getString("bio"));

                // 🔹 Add Achievements
                List<String> achievements = (List<String>) document.get("achievements");
                addAchievementsSection(achievements);
            }
        });
    }

    /**
     * Bio field (single EditText with save button)
     */
    private void addBioField(String value) {
        EditableRow row = new EditableRow("bio", value);
        container.addView(row);
        this.etBio = row.getEditText();
    }

    /**
     * Achievements section (dynamic list of EditTexts with + and - buttons)
     */
    /**
     * Achievements section (read-only text list)
     */
    private void addAchievementsSection(List<String> achievements) {
        achievementsContainer = new LinearLayout(this);
        achievementsContainer.setOrientation(LinearLayout.VERTICAL);

        // Section label
        TextView label = new TextView(this);
        label.setText("Achievements");
        label.setTextSize(16);
        label.setPadding(0, 20, 0, 10);
        label.setTypeface(null, Typeface.BOLD);
        achievementsContainer.addView(label);

        if (achievements != null && !achievements.isEmpty()) {
            for (String ach : achievements) {
                TextView achView = new TextView(this);
                achView.setText("• " + ach);
                achView.setPadding(10, 5, 0, 5);
                achievementsContainer.addView(achView);
            }
        } else {
            TextView noneView = new TextView(this);
            noneView.setText("No achievements yet.");
            noneView.setPadding(10, 5, 0, 5);
            achievementsContainer.addView(noneView);
        }

        container.addView(achievementsContainer);
    }

    /**
     * Add one achievement row
     */
    private void addAchievementField(String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        EditText et = new EditText(this);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        et.setHint("Achievement");
        if (value != null) et.setText(value);

        Button btnRemove = new Button(this);
        btnRemove.setText("-");
        btnRemove.setOnClickListener(v -> achievementsContainer.removeView(row));

        row.addView(et);
        row.addView(btnRemove);

        // Insert before the "+ Add Achievement" button
        achievementsContainer.addView(row, achievementsContainer.getChildCount() - 2);
    }

    /**
     * Save all achievements to Firestore
     */
    private void saveAchievements() {
        List<String> achievements = new ArrayList<>();

        // skip label (index 0), loop through achievement rows
        for (int i = 1; i < achievementsContainer.getChildCount() - 2; i++) {
            View row = achievementsContainer.getChildAt(i);
            if (row instanceof LinearLayout) {
                EditText et = (EditText) ((LinearLayout) row).getChildAt(0);
                String text = et.getText().toString().trim();
                if (!text.isEmpty()) {
                    achievements.add(text);
                }
            }
        }

        Map<String, Object> update = new HashMap<>();
        update.put("achievements", achievements);

        db.collection("users").document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Achievements updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update achievements", Toast.LENGTH_SHORT).show());
    }

    /**
     * Helper to safely set text and hint
     */
    private void setSafeText(EditText editText, String value, String fieldName) {
        if (value != null && !value.trim().isEmpty()) {
            editText.setText(value);
        } else {
            editText.setText(""); // leave empty → hint shows
        }
        editText.setHint(prettyLabel(fieldName));
    }

    /**
     * Convert "fullName" → "Full Name", "mobile" → "Mobile"
     */
    private String prettyLabel(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) return "";
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i == 0) {
                label.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                label.append(" ").append(c);
            } else {
                label.append(c);
            }
        }
        return label.toString();
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
        setSafeText(editText, value, fieldName);
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
            setSafeText(editText, value, fieldName);
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
                                        prettyLabel(fieldName) + " updated!", Toast.LENGTH_SHORT).show();

                                logChange(fieldName, newValue);

                                editText.setEnabled(false);
                                btnToggle.setText("Edit");
                                isEditing = false;
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(EditProfileActivity.this,
                                            "Failed to update " + prettyLabel(fieldName),
                                            Toast.LENGTH_SHORT).show());
                }
            });
        }

        public EditText getEditText() {
            return editText;
        }

        /**
         * Log changes to Firestore logs
         */
        private void logChange(String fieldName, String newValue) {
            Map<String, Object> log = new HashMap<>();
            log.put("username", currentUserEmail);
            log.put("datestamp", System.currentTimeMillis());
            log.put("change", prettyLabel(fieldName) + " updated to: " + newValue);

            FirebaseFirestore.getInstance()
                    .collection("logs")
                    .add(log);
        }


    }
}
