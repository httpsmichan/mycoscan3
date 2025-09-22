package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Verification extends AppCompatActivity {

    private EditText editInstitution, editGmail;
    private RadioGroup radioAffiliation, radioUploadOption;
    private Button btnChooseImage, btnChooseFile, btnSubmit;
    private TextView textUploadTitle, textFileName, textAlreadySubmitted;

    private Uri selectedUri = null;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Pickers
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedUri = uri;
                            textFileName.setText("Selected Image: " + uri.getLastPathSegment());
                            textFileName.setVisibility(TextView.VISIBLE);
                            btnSubmit.setVisibility(Button.VISIBLE);
                        }
                    });

    private final ActivityResultLauncher<String> pickFile =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedUri = uri;
                            textFileName.setText("Selected File: " + uri.getLastPathSegment());
                            textFileName.setVisibility(TextView.VISIBLE);
                            btnSubmit.setVisibility(Button.VISIBLE);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        editInstitution = findViewById(R.id.editInstitution);
        editGmail = findViewById(R.id.editGmail);
        radioAffiliation = findViewById(R.id.radioAffiliation);
        radioUploadOption = findViewById(R.id.radioUploadOption);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnChooseFile = findViewById(R.id.btnChooseFile);
        btnSubmit = findViewById(R.id.btnSubmit);
        textUploadTitle = findViewById(R.id.textUploadTitle);
        textFileName = findViewById(R.id.textFileName);
        textAlreadySubmitted = findViewById(R.id.textAlreadySubmitted);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Step 0: Check if already submitted
        checkExistingApplication();

        // Step 1: Affiliation toggle
        radioAffiliation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioYes) {
                editInstitution.setVisibility(EditText.VISIBLE);
            } else {
                editInstitution.setVisibility(EditText.GONE);
            }
            textUploadTitle.setVisibility(TextView.VISIBLE);
            radioUploadOption.setVisibility(RadioGroup.VISIBLE);
            editGmail.setVisibility(EditText.VISIBLE);
        });

        // Step 2: Show upload options
        radioUploadOption.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioImage) {
                btnChooseImage.setVisibility(Button.VISIBLE);
                btnChooseFile.setVisibility(Button.GONE);
            } else {
                btnChooseImage.setVisibility(Button.GONE);
                btnChooseFile.setVisibility(Button.VISIBLE);
            }
        });

        btnChooseImage.setOnClickListener(v -> pickImage.launch("image/*"));
        btnChooseFile.setOnClickListener(v -> pickFile.launch("application/pdf"));

        // Step 3: Submit
        btnSubmit.setOnClickListener(v -> {
            if (selectedUri == null) {
                Toast.makeText(this, "Please upload your TOR first!", Toast.LENGTH_SHORT).show();
                return;
            }

            String gmail = editGmail.getText().toString().trim();
            if (gmail.isEmpty() || !gmail.endsWith("@gmail.com")) {
                Toast.makeText(this, "Please enter a valid Gmail address!", Toast.LENGTH_SHORT).show();
                return;
            }

            String institution = editInstitution.getVisibility() == EditText.VISIBLE
                    ? editInstitution.getText().toString()
                    : "N/A";

            uploadToCloudinary(gmail, institution, selectedUri);
        });
    }

    private void checkExistingApplication() {
        db.collection("applications")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        findViewById(R.id.container).setVisibility(View.GONE);
                        textAlreadySubmitted.setVisibility(View.VISIBLE);

                        // Assuming only one application per user
                        String status = query.getDocuments().get(0).getString("status");

                        if ("approved".equalsIgnoreCase(status)) {
                            textAlreadySubmitted.setText("You are verified");
                            textAlreadySubmitted.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                            // ✅ Update user profile: verified flag
                            db.collection("users")
                                    .document(currentUser.getUid())
                                    .update("verified", true);

                            // ✅ Add achievement "verified expert"
                            db.collection("users")
                                    .document(currentUser.getUid())
                                    .update("achievements", com.google.firebase.firestore.FieldValue.arrayUnion("Verified Mushroom Expert"));

                        } else {
                            textAlreadySubmitted.setText("You have already submitted your application. Please wait for admin’s approval. We will notify you via email.");
                            textAlreadySubmitted.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        }
                    }
                });
    }

    private void uploadToCloudinary(String gmail, String institution, Uri fileUri) {
        // Determine type for Cloudinary
        String mimeType = getContentResolver().getType(fileUri);
        String resourceType = (mimeType != null && mimeType.equals("application/pdf")) ? "raw" : "auto";

        MediaManager.get().upload(fileUri)
                .unsigned("mycoscan")
                .option("resource_type", resourceType)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Toast.makeText(Verification.this, "Uploading...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String fileUrl = (String) resultData.get("secure_url");

                        Map<String, Object> application = new HashMap<>();
                        application.put("userId", currentUser.getUid());
                        application.put("gmail", gmail);
                        application.put("institution", institution);
                        application.put("fileUrl", fileUrl);
                        application.put("status", "pending");
                        application.put("timestamp", System.currentTimeMillis());

                        db.collection("applications")
                                .add(application)
                                .addOnSuccessListener(docRef -> {
                                    Toast.makeText(Verification.this, "Application submitted successfully!", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(Verification.this, "Failed to save application: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(Verification.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                })
                .dispatch();
    }
}
