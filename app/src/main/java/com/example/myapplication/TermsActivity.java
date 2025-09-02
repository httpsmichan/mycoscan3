package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class TermsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        db = FirebaseFirestore.getInstance();

        TextView termsText = findViewById(R.id.termsText);
        Button btnAccept = findViewById(R.id.btnAccept);

        termsText.setText(getString(R.string.accept_continue));

        btnAccept.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (userId != null) {
                DocumentReference userDoc = db.collection("users").document(userId);
                userDoc.update("acceptedTerms", true)
                        .addOnSuccessListener(unused -> {

                            Intent intent = new Intent(TermsActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        });
            }
        });

    }
}
