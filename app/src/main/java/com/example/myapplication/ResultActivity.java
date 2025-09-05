package com.example.myapplication;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ResultActivity extends AppCompatActivity {

    private TextView edibilityText;
    private TextView resultText;
    private ImageView resultImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultImage = findViewById(R.id.resultImage);
        resultText = findViewById(R.id.resultText);
        edibilityText = findViewById(R.id.edibility);

        String photoUriString = getIntent().getStringExtra("photoUri");
        String prediction = getIntent().getStringExtra("prediction");

        // Show the photo
        if (photoUriString != null) {
            Uri photoUri = Uri.parse(photoUriString);
            resultImage.setImageURI(photoUri);
        }

        // Show the identified mushroom name
        resultText.setText("Identified Mushroom: " + prediction);

        // 🔥 Fetch edibility from Firestore
        fetchEdibilityFromFirestore(prediction);
    }

    private void fetchEdibilityFromFirestore(String prediction) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("posts")
                .whereEqualTo("category", prediction)   // assuming you store name/class under "category"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String edibility = queryDocumentSnapshots.getDocuments()
                                .get(0)
                                .getString("edibility");

                        updateEdibilityUI(edibility);
                    } else {
                        updateEdibilityUI("Unknown");
                    }
                })
                .addOnFailureListener(e -> updateEdibilityUI("Unknown"));
    }

    private void updateEdibilityUI(String edibility) {
        edibilityText.setText("Edibility: " + edibility);

        if (edibility == null) {
            edibilityText.setBackgroundColor(Color.parseColor("#E0E0E0"));
            return;
        }

        switch (edibility.toLowerCase()) {
            case "edible":
                edibilityText.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "poisonous":
                edibilityText.setBackgroundColor(Color.parseColor("#F44336"));
                break;
            case "medicinal":
                edibilityText.setBackgroundColor(Color.parseColor("#2196F3"));
                break;
            case "inedible":
                edibilityText.setBackgroundColor(Color.parseColor("#FF9800"));
                break;
            default:
                edibilityText.setBackgroundColor(Color.parseColor("#9E9E9E"));
                break;
        }
    }
}
