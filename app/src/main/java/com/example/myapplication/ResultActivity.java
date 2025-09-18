package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ResultActivity extends AppCompatActivity {

    private TextView edibilityText;
    private TextView resultText;
    private TextView resultDescription;
    private TextView mushroomIdText;
    private ImageView resultImage;

    private TextView resultHabitat;
    private TextView resultCulinary;
    private TextView resultMedicinal;
    private TextView resultFacts;
    private TextView resultToxicity;
    private TextView resultSymptoms;
    private TextView resultDuration;
    private TextView resultLongTerm;
    private TextView resultCommonNames;
    private TextView mushroomEdibility; // for edibility + reason

    private String mushroomDocId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Initialize views
        resultImage = findViewById(R.id.resultImage);
        resultText = findViewById(R.id.resultText);
        edibilityText = findViewById(R.id.edibility);
        mushroomEdibility = findViewById(R.id.mushroomEdibility);
        resultDescription = findViewById(R.id.resultDescription);
        mushroomIdText = findViewById(R.id.mushroomIDText);

        resultHabitat = findViewById(R.id.resultHabitat);
        resultCulinary = findViewById(R.id.resultCulinary);
        resultMedicinal = findViewById(R.id.resultMedicinal);
        resultFacts = findViewById(R.id.resultFacts);
        resultToxicity = findViewById(R.id.resultToxicity);
        resultSymptoms = findViewById(R.id.resultSymptoms);
        resultDuration = findViewById(R.id.resultDuration);
        resultLongTerm = findViewById(R.id.resultLongTerm);
        resultCommonNames = findViewById(R.id.resultCommonNames);

        // Get intent extras
        String photoUriString = getIntent().getStringExtra("photoUri");
        String prediction = getIntent().getStringExtra("prediction");

        String cleanedPrediction = prediction != null ? prediction.replaceFirst("^\\d+\\s+", "").trim() : "";

        Log.d("ResultActivity", "Original prediction: '" + prediction + "'");
        Log.d("ResultActivity", "Cleaned prediction: '" + cleanedPrediction + "'");

        resultText.setText("Identified Mushroom: " + cleanedPrediction);

        if (photoUriString != null) {
            Uri photoUri = Uri.parse(photoUriString);
            resultImage.setImageURI(photoUri);
        }

        fetchMushroomDetails(cleanedPrediction);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\p{C}|\\s+", " ").trim();
    }

    private void fetchMushroomDetails(String cleanedPrediction) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("mushroom-encyclopedia")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;

                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("mushroomName");
                        String mushroomID = doc.getString("mushroomID");

                        if (normalize(name).equalsIgnoreCase(normalize(cleanedPrediction))) {
                            mushroomDocId = doc.getId();

                            String edibility = doc.getString("edibility");
                            String reason = doc.getString("reason");
                            String description = doc.getString("description");

                            // Fetch all list/string fields
                            Object habitats = doc.get("habitats");
                            Object culinary = doc.get("culinaryUses");
                            Object medicinal = doc.get("medicinalUses");
                            Object facts = doc.get("funFacts");
                            Object toxicity = doc.get("toxicity");
                            Object symptoms = doc.get("onsetSymptoms");
                            Object duration = doc.get("durationOfEffects");
                            Object longTerm = doc.get("longTermEffects");
                            Object commonnames = doc.get("commonNames");

                            // Update UI
                            updateEdibilityUI(edibility);
                            updateEdibilityReasonUI(edibility, reason);
                            updateDescriptionUI(description != null ? description : "No description found.");
                            updateMushroomIdUI(mushroomID);

                            updateListTextView(resultHabitat, habitats, "Common Habitat");
                            updateListTextView(resultCulinary, culinary, "Culinary Uses");
                            updateListTextView(resultMedicinal, medicinal, "Medicinal Uses");
                            updateListTextView(resultFacts, facts, "Facts");
                            updateListTextView(resultToxicity, toxicity, "Toxicity");
                            updateListTextView(resultSymptoms, symptoms, "Onset Symptoms");
                            updateListTextView(resultDuration, duration, "Duration of Effects");
                            updateListTextView(resultLongTerm, longTerm, "Long Term Effects");
                            updateListTextView(resultCommonNames, commonnames, "Common Names");

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        updateEdibilityUI("Unknown");
                        updateEdibilityReasonUI("Unknown", null);
                        updateDescriptionUI("No description available.");
                        updateMushroomIdUI("N/A");

                        hideAllListViews();
                    }
                })
                .addOnFailureListener(e -> {
                    updateEdibilityUI("Unknown");
                    updateEdibilityReasonUI("Unknown", null);
                    updateDescriptionUI("Failed to load description.");
                    updateMushroomIdUI("N/A");

                    hideAllListViews();
                    Log.e("ResultActivity", "Firestore query failed", e);
                });
    }

    private void hideAllListViews() {
        resultHabitat.setVisibility(TextView.GONE);
        resultCulinary.setVisibility(TextView.GONE);
        resultMedicinal.setVisibility(TextView.GONE);
        resultFacts.setVisibility(TextView.GONE);
        resultToxicity.setVisibility(TextView.GONE);
        resultSymptoms.setVisibility(TextView.GONE);
        resultDuration.setVisibility(TextView.GONE);
        resultLongTerm.setVisibility(TextView.GONE);
        resultCommonNames.setVisibility(TextView.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void updateListTextView(TextView textView, Object fieldValue, String label) {
        StringBuilder text = new StringBuilder();

        if (fieldValue instanceof String) {
            text.append((String) fieldValue);
        } else if (fieldValue instanceof java.util.List) {
            for (Object item : (java.util.List<?>) fieldValue) {
                text.append(item.toString()).append("\n");
            }
        }

        String finalText = text.toString().trim();

        if (finalText.isEmpty()) {
            textView.setText("N/A");
            textView.setVisibility(TextView.GONE);
        } else {
            textView.setText(label + ":\n" + finalText);
            textView.setVisibility(TextView.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateDescriptionUI(String description) {
        resultDescription.setText("Description: " + description);
    }

    @SuppressLint("SetTextI18n")
    private void updateMushroomIdUI(String mushroomID) {
        mushroomIdText.setText("Mushroom ID: " + mushroomID);
    }

    @SuppressLint("SetTextI18n")
    private void updateEdibilityUI(String edibility) {
        if (edibility == null) {
            edibilityText.setText("Edibility: N/A");
            edibilityText.setBackgroundColor(Color.parseColor("#E0E0E0"));
            return;
        }

        // Normalize Firestore codes
        String displayValue;
        switch (edibility.toLowerCase()) {
            case "ediblew":
                displayValue = "Edible with Caution";
                edibilityText.setBackgroundColor(Color.parseColor("#FFC107")); // amber
                break;
            case "inediblemed":
                displayValue = "Inedible (Medicinal)";
                edibilityText.setBackgroundColor(Color.parseColor("#2196F3")); // blue
                break;
            case "edible":
                displayValue = "Edible";
                edibilityText.setBackgroundColor(Color.parseColor("#4CAF50")); // green
                break;
            case "poisonous":
                displayValue = "Poisonous";
                edibilityText.setBackgroundColor(Color.parseColor("#F44336")); // red
                break;
            case "medicinal":
                displayValue = "Medicinal";
                edibilityText.setBackgroundColor(Color.parseColor("#3F51B5")); // indigo
                break;
            case "inedible":
                displayValue = "Inedible";
                edibilityText.setBackgroundColor(Color.parseColor("#FF9800")); // orange
                break;
            default:
                displayValue = edibility;
                edibilityText.setBackgroundColor(Color.parseColor("#9E9E9E")); // gray
                break;
        }

        edibilityText.setText("Edibility: " + displayValue);
    }

    @SuppressLint("SetTextI18n")
    private void updateEdibilityReasonUI(String edibility, String reason) {
        if (edibility == null) edibility = "N/A";

        // Apply same normalization for the combined field
        String displayValue;
        switch (edibility.toLowerCase()) {
            case "ediblew":
                displayValue = "Edible with Caution";
                break;
            case "inediblemed":
                displayValue = "Inedible (Medicinal)";
                break;
            default:
                displayValue = edibility;
                break;
        }

        if (reason == null || reason.isEmpty()) {
            mushroomEdibility.setText("Edibility: " + displayValue);
        } else {
            mushroomEdibility.setText("Edibility: " + displayValue + ", " + reason);
        }
    }
}
