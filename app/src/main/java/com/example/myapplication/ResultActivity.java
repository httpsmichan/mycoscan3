package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    private TextView title1;
    private TextView title2, unknown, reminder, headerDisclaimer, shareCommunity;
    private LinearLayout charLayout;

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

        title1 = findViewById(R.id.title1);
        title2 = findViewById(R.id.title2);
        charLayout = findViewById(R.id.charLayout);
        unknown = findViewById(R.id.unknown);
        reminder = findViewById(R.id.reminder);
        headerDisclaimer = findViewById(R.id.headerDisclaimer);
        shareCommunity = findViewById(R.id.shareCommunity);

        // Add this at the end of onCreate() method in ResultActivity.java

        shareCommunity.setOnClickListener(v -> {
            // Get the mushroom name
            String mushroomName = mushroomNameText.getText().toString();

            // Get the photo URI
            String photoUriString = getIntent().getStringExtra("photoUri");

            // Get location data from intent (if it was passed to ResultActivity)
            double latitude = getIntent().getDoubleExtra("latitude", 0.0);
            double longitude = getIntent().getDoubleExtra("longitude", 0.0);

            // Get edibility/category
            String edibility = "Unknown / Needs ID";
            if (mushroomEdibility.getText() != null) {
                String edibilityText = mushroomEdibility.getText().toString();
                if (edibilityText.startsWith("Edible with Caution")) {
                    edibility = "Edible";
                } else if (edibilityText.startsWith("Edible")) {
                    edibility = "Edible";
                } else if (edibilityText.startsWith("Poisonous")) {
                    edibility = "Poisonous";
                } else if (edibilityText.startsWith("Inedible (Medicinal)")) {
                    edibility = "Medicinal";
                } else if (edibilityText.contains("Unknown")) {
                    edibility = "Unknown / Needs ID";
                }
            }

            // Get description
            String description = "";
            if (resultDescription.getText() != null) {
                String descText = resultDescription.getText().toString();
                if (descText.startsWith("Description: ")) {
                    description = descText.substring(13);
                } else {
                    description = descText;
                }
            }

            // Navigate to TabbedActivity with Upload tab selected
            Intent intent = new Intent(ResultActivity.this, TabbedActivity.class);
            intent.putExtra("openUploadTab", true);
            intent.putExtra("mushroomType", mushroomName);
            intent.putExtra("category", edibility);
            intent.putExtra("description", description);
            intent.putExtra("photoUri", photoUriString);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Get intent extras first
        String photoUriString = getIntent().getStringExtra("photoUri");
        String prediction = getIntent().getStringExtra("prediction");
        float confidence = getIntent().getFloatExtra("confidence", -1f);

        // Load photo FIRST, before any confidence checks
        if (photoUriString != null) {
            Uri photoUri = Uri.parse(photoUriString);
            resultImage.setImageURI(photoUri);
        }

        String cleanedPrediction = prediction != null ? prediction.replaceFirst("^\\d+\\s+", "").trim() : "";

        if (confidence < 0.8f || cleanedPrediction.isEmpty()) {
            unknown.setVisibility(View.VISIBLE);
            hideAllExceptImageAndUnknown();
            return;
        }

        mushroomNameText.setText(cleanedPrediction);

        if (confidence >= 0) {
            int percentage = Math.round(confidence * 100);
            mushroomAccuracyBar.setProgress(percentage);
            mushroomAccuracyText.setText(percentage + "%");
        } else {
            mushroomAccuracyBar.setProgress(0);
            mushroomAccuracyText.setText("0%");
        }

        fetchMushroomDetails(cleanedPrediction);
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\p{C}|\\s+", " ").trim();
    }

    private void fetchMushroomDetails(String cleanedPrediction) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Handle class 25 = Unknown before querying Firestore
        if (cleanedPrediction.equalsIgnoreCase("Unknown")) {
            mushroomNameText.setText("Unknown");
            updateEdibilityUI("Unknown", null);
            updateDescriptionUI("No information available for this class.");
            hideAllContainers();
            return;
        }

        db.collection("mushroom-encyclopedia")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean found = false;

                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("mushroomName");

                        if (namesMatch(name, cleanedPrediction)) {
                            mushroomDocId = doc.getId();

                            String edibility = doc.getString("edibility");
                            String reason = doc.getString("reason");
                            String description = doc.getString("description");

                            String reasonTrim = reason != null ? reason.trim() : null;

                            Log.d(TAG, "Found doc id=" + mushroomDocId +
                                    " mushroomName='" + name +
                                    "' edibility='" + edibility +
                                    "' reason='" + reasonTrim + "'");

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

                            updateEdibilityUI(edibility, reasonTrim);
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

                            // Only update characteristics if prediction is valid and not Unknown
                            if (cleanedPrediction != null && !cleanedPrediction.isEmpty() &&
                                    !cleanedPrediction.equalsIgnoreCase("Unknown")) {
                                updateCharacteristics(characteristics, cleanedPrediction);
                            } else {
                                charLayout.setVisibility(View.GONE);
                                resultFirst.setVisibility(View.GONE);
                                resultSecond.setVisibility(View.GONE);
                                headerDisclaimer.setVisibility(View.GONE);
                                reminder.setVisibility(View.GONE);
                                unknown.setVisibility(View.VISIBLE);
                            }

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
                            showContainerIfNotNull(
                                    mushroomCommonNamesText.getParent() instanceof LinearLayout
                                            ? (LinearLayout)mushroomCommonNamesText.getParent()
                                            : null,
                                    commonNames
                            );

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        Log.d(TAG, "No matching mushroom document found for prediction: " + cleanedPrediction);
                        updateEdibilityUI("Unknown", null);
                        updateDescriptionUI("No description available.");
                        hideAllContainers();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch mushroom-encyclopedia docs", e);
                    updateEdibilityUI("Unknown", null);
                    updateDescriptionUI("Failed to load description.");
                    hideAllContainers();
                });
    }

    private String cleanName(String name) {
        if (name == null) return "";
        // Remove parentheses and trim
        return name.replaceAll("\\(.*?\\)", "").trim();
    }

    private boolean namesMatch(String a, String b) {
        return normalize(cleanName(a)).equalsIgnoreCase(normalize(cleanName(b)));
    }

    private void hideAllExceptImageAndUnknown() {
        // Hide the mushroom info container
        findViewById(R.id.mushroomInfoContainer).setVisibility(View.GONE);

        // Hide every container
        edibilityContainer.setVisibility(View.GONE);
        descriptionContainer.setVisibility(View.GONE);
        habitatContainer.setVisibility(View.GONE);
        culinaryContainer.setVisibility(View.GONE);
        medicinalContainer.setVisibility(View.GONE);
        toxicityContainer.setVisibility(View.GONE);
        symptomsContainer.setVisibility(View.GONE);
        durationContainer.setVisibility(View.GONE);
        longTermContainer.setVisibility(View.GONE);
        factsContainer.setVisibility(View.GONE);

        // Hide text sections
        mushroomNameText.setVisibility(View.GONE);
        mushroomCommonNamesText.setVisibility(View.GONE);
        resultHabitat.setVisibility(View.GONE);
        resultCulinary.setVisibility(View.GONE);
        resultMedicinal.setVisibility(View.GONE);
        resultFacts.setVisibility(View.GONE);
        resultToxicity.setVisibility(View.GONE);
        resultSymptoms.setVisibility(View.GONE);
        resultDuration.setVisibility(View.GONE);
        resultLongTerm.setVisibility(View.GONE);
        mushroomEdibility.setVisibility(View.GONE);
        resultDescription.setVisibility(View.GONE);
        resultFirst.setVisibility(View.GONE);
        resultSecond.setVisibility(View.GONE);

        // Hide titles, disclaimers, reminders, accuracy
        title1.setVisibility(View.GONE);
        title2.setVisibility(View.GONE);
        headerDisclaimer.setVisibility(View.GONE);
        reminder.setVisibility(View.GONE);
        mushroomAccuracyBar.setVisibility(View.GONE);
        mushroomAccuracyText.setVisibility(View.GONE);

        // Hide characteristics block
        charLayout.setVisibility(View.GONE);
        shareCommunity.setVisibility(View.GONE);

        // Leave resultImage and unknown visible
        findViewById(R.id.noInformationLayout).setVisibility(View.VISIBLE);
        resultImage.setVisibility(View.VISIBLE);
        unknown.setVisibility(View.VISIBLE);
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
    private void updateCharacteristics(Object fieldValue, String prediction) {
        // Handle case when prediction is null or Unknown
        if (prediction == null || prediction.trim().isEmpty() || prediction.equalsIgnoreCase("Unknown")) {
            charLayout.setVisibility(LinearLayout.GONE);
            resultFirst.setVisibility(TextView.GONE);
            resultSecond.setVisibility(TextView.GONE);

            headerDisclaimer.setVisibility(View.GONE);
            reminder.setVisibility(View.GONE);

            unknown.setVisibility(View.VISIBLE);
            return;
        }

        // Hide "Unknown" message for valid prediction
        unknown.setVisibility(View.GONE);

        if (fieldValue instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) fieldValue;

            if (list.isEmpty()) {
                charLayout.setVisibility(LinearLayout.GONE);
                return;
            }

            // First item
            if (list.size() > 0 && list.get(0) != null && !list.get(0).toString().trim().isEmpty()) {
                resultFirst.setText(list.get(0).toString());
                resultFirst.setVisibility(TextView.VISIBLE);
            } else {
                resultFirst.setVisibility(TextView.GONE);
            }

            // Second item
            if (list.size() > 1 && list.get(1) != null && !list.get(1).toString().trim().isEmpty()) {
                resultSecond.setText(list.get(1).toString());
                resultSecond.setVisibility(TextView.VISIBLE);
            } else {
                resultSecond.setVisibility(TextView.GONE);
            }

            // Show charLayout only if at least one is visible
            if (resultFirst.getVisibility() == TextView.VISIBLE || resultSecond.getVisibility() == TextView.VISIBLE) {
                charLayout.setVisibility(LinearLayout.VISIBLE);
            } else {
                charLayout.setVisibility(LinearLayout.GONE);
            }

        } else if (fieldValue instanceof String) {
            String value = ((String) fieldValue).trim();

            if (value.isEmpty()) {
                charLayout.setVisibility(LinearLayout.GONE);
                return;
            }

            resultFirst.setText(value);
            resultFirst.setVisibility(TextView.VISIBLE);
            resultSecond.setVisibility(TextView.GONE);

            charLayout.setVisibility(LinearLayout.VISIBLE);

        } else {
            // Not a list or string → hide entire block
            charLayout.setVisibility(LinearLayout.GONE);
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

    @SuppressLint("SetTextI18n")
    private void updateEdibilityUI(String edibility, String reason) {
        if (edibility == null) edibility = "N/A";

        String displayValue;
        int bgColor = Color.TRANSPARENT;
        int borderColor = Color.TRANSPARENT;

        switch (edibility.toLowerCase()) {
            case "edible":
                displayValue = "Edible";
                bgColor = Color.parseColor("#804CAF50");
                borderColor = Color.parseColor("#4CAF50");
                break;
            case "poisonous":
                displayValue = "Poisonous";
                bgColor = Color.parseColor("#80D11406");
                borderColor = Color.parseColor("#D11406");
                break;
            case "ediblew":
                displayValue = "Edible with Caution";
                bgColor = Color.parseColor("#80FFC107");
                borderColor = Color.parseColor("#FFC107");
                break;
            case "inediblemed":
                displayValue = "Inedible (Medicinal)";
                bgColor = Color.parseColor("#80857D7D");
                borderColor = Color.parseColor("#857D7D");
                break;
            case "unknown":
                displayValue = "Unknown";
                bgColor = Color.parseColor("#80808080");
                borderColor = Color.parseColor("#A0A0A0");
                break;
            default:
                displayValue = edibility;
                break;
        }

        if (reason != null && !reason.isEmpty()) {
            String combined = displayValue + ": " + reason;
            mushroomEdibility.setText(combined);
            Log.d(TAG, "Setting mushroomEdibility text -> " + combined);
        } else {
            mushroomEdibility.setText(displayValue);
            Log.d(TAG, "Setting mushroomEdibility text -> " + displayValue + " (no reason)");
        }

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(bgColor);
        drawable.setCornerRadius(20f);
        drawable.setStroke(2, borderColor);
        edibilityContainer.setBackground(drawable);
    }
}