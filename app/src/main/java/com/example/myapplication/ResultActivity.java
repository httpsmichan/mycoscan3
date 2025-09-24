package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ResultActivity extends AppCompatActivity {

    private TextView edibilityText;
    private TextView mushroomEdibility;
    private TextView resultDescription;
    private ImageView resultImage;

    private TextView resultHabitat;
    private TextView resultCulinary;
    private TextView mushroomNameText;
    private TextView resultMedicinal;
    private TextView resultFacts;
    private TextView resultToxicity;
    private TextView resultSymptoms;
    private TextView resultDuration;
    private TextView resultLongTerm;
    private TextView mushroomCommonNamesText;
    private ProgressBar mushroomAccuracyBar;
    private TextView mushroomAccuracyText;
    private TextView resultFirst;
    private TextView resultSecond;

    private String mushroomDocId;
    private LinearLayout edibilityContainer;
    private LinearLayout descriptionContainer;
    private LinearLayout habitatContainer;
    private LinearLayout culinaryContainer;
    private LinearLayout medicinalContainer;
    private LinearLayout toxicityContainer;
    private LinearLayout symptomsContainer;
    private LinearLayout durationContainer;
    private LinearLayout longTermContainer;
    private LinearLayout factsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Initialize views
        resultImage = findViewById(R.id.resultImage);
        edibilityText = findViewById(R.id.edibility);
        mushroomEdibility = findViewById(R.id.mushroomEdibility);
        resultDescription = findViewById(R.id.resultDescription);

        resultHabitat = findViewById(R.id.resultHabitat);
        resultCulinary = findViewById(R.id.resultCulinary);
        resultMedicinal = findViewById(R.id.resultMedicinal);
        resultFacts = findViewById(R.id.resultFacts);
        resultToxicity = findViewById(R.id.resultToxicity);
        resultSymptoms = findViewById(R.id.resultSymptoms);
        resultDuration = findViewById(R.id.resultDuration);
        resultLongTerm = findViewById(R.id.resultLongTerm);
        mushroomNameText = findViewById(R.id.mushroomNameText);
        mushroomCommonNamesText = findViewById(R.id.mushroomCommonNamesText);

        mushroomAccuracyBar = findViewById(R.id.mushroomAccuracyBar);
        mushroomAccuracyText = findViewById(R.id.mushroomAccuracyText);
        resultFirst = findViewById(R.id.resultFirst);
        resultSecond = findViewById(R.id.resultSecond);

        edibilityContainer = findViewById(R.id.edibilityContainer);
        descriptionContainer = findViewById(R.id.descriptionContainer);
        habitatContainer = findViewById(R.id.habitatContainer);
        culinaryContainer = findViewById(R.id.culinaryContainer);
        medicinalContainer = findViewById(R.id.medicinalContainer);
        toxicityContainer = findViewById(R.id.toxicityContainer);
        symptomsContainer = findViewById(R.id.symptomsContainer);
        durationContainer = findViewById(R.id.durationContainer);
        longTermContainer = findViewById(R.id.longTermContainer);
        factsContainer = findViewById(R.id.factsContainer);

        // Get intent extras
        String photoUriString = getIntent().getStringExtra("photoUri");
        String prediction = getIntent().getStringExtra("prediction");
        float confidence = getIntent().getFloatExtra("confidence", -1f);

        String cleanedPrediction = prediction != null ? prediction.replaceFirst("^\\d+\\s+", "").trim() : "";

        mushroomNameText.setText(cleanedPrediction);

        if (confidence >= 0) {
            int percentage = Math.round(confidence * 100);
            mushroomAccuracyBar.setProgress(percentage);
            mushroomAccuracyText.setText(percentage + "%");
        } else {
            mushroomAccuracyBar.setProgress(0);
            mushroomAccuracyText.setText("0%");
        }

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

                        if (normalize(name).equalsIgnoreCase(normalize(cleanedPrediction))) {
                            mushroomDocId = doc.getId();

                            String edibility = doc.getString("edibility");
                            String reason = doc.getString("reason");
                            String description = doc.getString("description");

                            Object commonNames = doc.get("commonNames");
                            Object habitats = doc.get("habitats");
                            Object culinary = doc.get("culinaryUses");
                            Object medicinal = doc.get("medicinalUses");
                            Object facts = doc.get("funFacts");
                            Object toxicity = doc.get("toxicity");
                            Object onset = doc.get("onset");
                            Object duration = doc.get("duration");
                            Object longTerm = doc.get("longTerm");
                            Object characteristics = doc.get("characteristics");

                            updateEdibilityUI(edibility);
                            updateEdibilityReasonUI(edibility, reason);
                            updateDescriptionUI(description != null ? description : "No description found.");

                            updateListTextView(mushroomCommonNamesText, commonNames);
                            updateListTextView(resultHabitat, habitats);
                            updateListTextView(resultCulinary, culinary);
                            updateListTextView(resultMedicinal, medicinal);
                            updateListTextView(resultFacts, facts);
                            updateListTextView(resultToxicity, toxicity);
                            updateListTextView(resultSymptoms, onset);
                            updateListTextView(resultDuration, duration);
                            updateListTextView(resultLongTerm, longTerm);

                            updateCharacteristics(characteristics);

                            showContainerIfNotNull(edibilityContainer, edibility);
                            showContainerIfNotNull(descriptionContainer, description);
                            showContainerIfNotNull(habitatContainer, habitats);
                            showContainerIfNotNull(culinaryContainer, culinary);
                            showContainerIfNotNull(medicinalContainer, medicinal);
                            showContainerIfNotNull(toxicityContainer, toxicity);
                            showContainerIfNotNull(symptomsContainer, onset);
                            showContainerIfNotNull(durationContainer, duration);
                            showContainerIfNotNull(longTermContainer, longTerm);
                            showContainerIfNotNull(factsContainer, facts);
                            showContainerIfNotNull(mushroomCommonNamesText.getParent() instanceof LinearLayout ? (LinearLayout)mushroomCommonNamesText.getParent() : null, commonNames);

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        updateEdibilityUI("Unknown");
                        updateEdibilityReasonUI("Unknown", null);
                        updateDescriptionUI("No description available.");
                        hideAllContainers();
                    }
                })
                .addOnFailureListener(e -> {
                    updateEdibilityUI("Unknown");
                    updateEdibilityReasonUI("Unknown", null);
                    updateDescriptionUI("Failed to load description.");
                    hideAllContainers();
                });
    }

    private void hideAllContainers() {
        edibilityContainer.setVisibility(LinearLayout.GONE);
        descriptionContainer.setVisibility(LinearLayout.GONE);
        habitatContainer.setVisibility(LinearLayout.GONE);
        culinaryContainer.setVisibility(LinearLayout.GONE);
        medicinalContainer.setVisibility(LinearLayout.GONE);
        toxicityContainer.setVisibility(LinearLayout.GONE);
        symptomsContainer.setVisibility(LinearLayout.GONE);
        durationContainer.setVisibility(LinearLayout.GONE);
        longTermContainer.setVisibility(LinearLayout.GONE);
        factsContainer.setVisibility(LinearLayout.GONE);
        if (mushroomCommonNamesText.getParent() instanceof LinearLayout) {
            ((LinearLayout) mushroomCommonNamesText.getParent()).setVisibility(LinearLayout.GONE);
        }
    }

    private void showContainerIfNotNull(LinearLayout container, Object data) {
        if (container == null) return;

        if (data == null) container.setVisibility(LinearLayout.GONE);
        else if (data instanceof String && ((String) data).trim().isEmpty()) container.setVisibility(LinearLayout.GONE);
        else if (data instanceof java.util.List && ((java.util.List<?>) data).isEmpty()) container.setVisibility(LinearLayout.GONE);
        else container.setVisibility(LinearLayout.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void updateCharacteristics(Object fieldValue) {
        if (fieldValue instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) fieldValue;
            resultFirst.setText(list.size() > 0 ? list.get(0).toString() : "N/A");
            resultSecond.setText(list.size() > 1 ? list.get(1).toString() : "N/A");
            resultFirst.setVisibility(TextView.VISIBLE);
            resultSecond.setVisibility(TextView.VISIBLE);
        } else if (fieldValue instanceof String) {
            resultFirst.setText((String) fieldValue);
            resultSecond.setText("N/A");
            resultFirst.setVisibility(TextView.VISIBLE);
            resultSecond.setVisibility(TextView.VISIBLE);
        } else {
            resultFirst.setText("N/A");
            resultSecond.setText("N/A");
            resultFirst.setVisibility(TextView.GONE);
            resultSecond.setVisibility(TextView.GONE);
        }
    }

    private void updateListTextView(TextView textView, Object fieldValue) {
        StringBuilder text = new StringBuilder();

        if (fieldValue instanceof String) text.append((String) fieldValue);
        else if (fieldValue instanceof java.util.List) {
            for (Object item : (java.util.List<?>) fieldValue) text.append(item.toString()).append("\n");
        }

        String finalText = text.toString().trim();

        if (finalText.isEmpty()) textView.setVisibility(TextView.GONE);
        else {
            textView.setText(finalText);
            textView.setVisibility(TextView.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateDescriptionUI(String description) {
        resultDescription.setText("Description: " + description);
    }

    private void updateEdibilityUI(String edibility) {
        String displayValue = (edibility == null) ? "N/A" : edibility;
        int bgColor = Color.TRANSPARENT;
        int borderColor = Color.TRANSPARENT;

        if (edibility != null) {
            switch (edibility.toLowerCase()) {
                case "edible":
                    displayValue = "Edible";
                    bgColor = Color.parseColor("#804CAF50"); // semi-transparent green
                    borderColor = Color.parseColor("#4CAF50"); // opaque green
                    break;
                case "poisonous":
                    displayValue = "Poisonous";
                    bgColor = Color.parseColor("#80D11406"); // semi-transparent red
                    borderColor = Color.parseColor("#D11406"); // opaque red
                    break;
                default:
                    displayValue = edibility;
                    bgColor = Color.TRANSPARENT;
                    borderColor = Color.TRANSPARENT;
                    break;
            }
        }

        mushroomEdibility.setText(displayValue);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(20f);
        drawable.setStroke(2, borderColor); // 2px border with opaque color
        edibilityContainer.setBackground(drawable);
    }

    @SuppressLint("SetTextI18n")
    private void updateEdibilityReasonUI(String edibility, String reason) {
        if (edibility == null) edibility = "N/A";

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

        if (reason == null || reason.isEmpty()) mushroomEdibility.setText("Edibility: " + displayValue);
        else mushroomEdibility.setText("Edibility: " + displayValue + ", " + reason);
    }
}
