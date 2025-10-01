package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EncyclopediaActivity extends AppCompatActivity {

    private TextView mushroomNameView, mushroomEdibility, resultDescription, resultHabitat,
            resultFirst, resultSecond, resultCulinary, resultMedicinal,
            resultToxicity, resultSymptoms, resultDuration, resultLongTerm, resultFacts;

    private LinearLayout edibilityContainer, descriptionContainer, habitatContainer, charLayout,
            culinaryContainer, medicinalContainer, toxicityContainer, symptomsContainer,
            durationContainer, longTermContainer, factsContainer;

    private ViewPager2 imagePager;
    private MushroomImageAdapter imageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

        // 🌱 Initialize views
        mushroomNameView = findViewById(R.id.mushroomName);
        mushroomEdibility = findViewById(R.id.mushroomEdibility);
        resultDescription = findViewById(R.id.resultDescription);
        resultHabitat = findViewById(R.id.resultHabitat);
        resultFirst = findViewById(R.id.resultFirst);
        resultSecond = findViewById(R.id.resultSecond);
        resultCulinary = findViewById(R.id.resultCulinary);
        resultMedicinal = findViewById(R.id.resultMedicinal);
        resultToxicity = findViewById(R.id.resultToxicity);
        resultSymptoms = findViewById(R.id.resultSymptoms);
        resultDuration = findViewById(R.id.resultDuration);
        resultLongTerm = findViewById(R.id.resultLongTerm);
        resultFacts = findViewById(R.id.resultFacts);

        // Containers
        edibilityContainer = findViewById(R.id.edibilityContainer);
        descriptionContainer = findViewById(R.id.descriptionContainer);
        habitatContainer = findViewById(R.id.habitatContainer);
        charLayout = findViewById(R.id.charLayout);
        culinaryContainer = findViewById(R.id.culinaryContainer);
        medicinalContainer = findViewById(R.id.medicinalContainer);
        toxicityContainer = findViewById(R.id.toxicityContainer);
        symptomsContainer = findViewById(R.id.symptomsContainer);
        durationContainer = findViewById(R.id.durationContainer);
        longTermContainer = findViewById(R.id.longTermContainer);
        factsContainer = findViewById(R.id.factsContainer);

        // Image pager
        imagePager = findViewById(R.id.mushroomImagePager);

        // Get the mushroom name from intent
        String mushroomName = getIntent().getStringExtra("mushroomName");
        if (mushroomName != null) {
            mushroomNameView.setText(mushroomName);
            loadMushroomData(mushroomName);
        }
    }

    private void loadMushroomData(String mushroomName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("mushroom-encyclopedia")
                .whereEqualTo("mushroomName", mushroomName)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        // 🌱 Set Edibility
                        setTextOrHide(doc.get("edibility"), mushroomEdibility, edibilityContainer);

                        // 🌱 Set Description
                        setTextOrHide(doc.get("description"), resultDescription, descriptionContainer);

                        // 🌱 Set Habitat
                        setArrayOrHide((List<String>) doc.get("habitats"), resultHabitat, habitatContainer);

                        // 🌱 Characteristics
                        List<String> characteristics = (List<String>) doc.get("characteristics");
                        if (characteristics != null && !characteristics.isEmpty()) {
                            resultFirst.setText(characteristics.size() > 0 ? characteristics.get(0) : "");
                            resultSecond.setText(characteristics.size() > 1 ? characteristics.get(1) : "");
                        } else {
                            charLayout.setVisibility(View.GONE);
                        }

                        // 🌱 Culinary
                        setArrayOrHide((List<String>) doc.get("culinaryUses"), resultCulinary, culinaryContainer);

                        // 🌱 Medicinal
                        setArrayOrHide((List<String>) doc.get("medicinalUses"), resultMedicinal, medicinalContainer);

                        // 🌱 Toxicity
                        setTextOrHide(doc.get("toxicity"), resultToxicity, toxicityContainer);

                        // 🌱 Symptoms
                        setArrayOrHide((List<String>) doc.get("onset"), resultSymptoms, symptomsContainer);

                        // 🌱 Duration
                        setTextOrHide(doc.get("duration"), resultDuration, durationContainer);

                        // 🌱 Long Term Effects
                        setTextOrHide(doc.get("longTerm"), resultLongTerm, longTermContainer);

                        // 🌱 Facts
                        setArrayOrHide((List<String>) doc.get("funFacts"), resultFacts, factsContainer);

                        // 🌱 Images
                        List<String> images = (List<String>) doc.get("images");
                        if (images != null && !images.isEmpty()) {
                            imageAdapter = new MushroomImageAdapter(this, images);
                            imagePager.setAdapter(imageAdapter);

// Show multiple pages with space between
                            imagePager.setClipToPadding(false);
                            imagePager.setClipChildren(false);
                            imagePager.setOffscreenPageLimit(3);

                            int pageMarginPx = getResources().getDimensionPixelOffset(R.dimen.viewpager_page_margin);
                            int offsetPx = getResources().getDimensionPixelOffset(R.dimen.viewpager_page_offset);

                            imagePager.setPageTransformer((page, position) -> {
                                float myOffset = position * -(2 * offsetPx + pageMarginPx);
                                if (imagePager.getOrientation() == ViewPager2.ORIENTATION_HORIZONTAL) {
                                    page.setTranslationX(myOffset);
                                } else {
                                    page.setTranslationY(myOffset);
                                }
                            });


                        } else {
                            imagePager.setVisibility(View.GONE);
                        }

                    }
                });
    }

    // ✅ Now accepts Object instead of String
    private void setTextOrHide(Object value, TextView textView, LinearLayout container) {
        if (value != null && !value.toString().trim().isEmpty()) {
            textView.setText(value.toString());
            container.setVisibility(View.VISIBLE);
        } else {
            container.setVisibility(View.GONE);
        }
    }

    private void setArrayOrHide(List<String> values, TextView textView, LinearLayout container) {
        if (values != null && !values.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String s : values) {
                builder.append("• ").append(s).append("\n");
            }
            textView.setText(builder.toString().trim());
            container.setVisibility(View.VISIBLE);
        } else {
            container.setVisibility(View.GONE);
        }
    }
}
