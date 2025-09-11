package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReportActivity extends AppCompatActivity {

    private EditText etReason;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        etReason = findViewById(R.id.etReason);
        btnSubmit = findViewById(R.id.btnSubmitReport);

        // Get postId from intent
        String postId = getIntent().getStringExtra("postId");

        btnSubmit.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : "anonymous";

            Report report = new Report(postId, userId, System.currentTimeMillis(), reason);

            FirebaseFirestore.getInstance()
                    .collection("reports")
                    .add(report)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show());
        });
    }
}
