package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UploadFragment extends Fragment {

    private EditText etMushroomType, etDescription;
    private Spinner spinnerCategory;
    private Button btnPickImage, btnGetLocation, btnSubmit;
    private Uri imageUri;
    private String userLocation = "Unknown";

    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQUEST_LOCATION = 101;
    private ImageView imagePreview;
    private TextView tvLatitude, tvLongitude;
    private double latitude = 0.0;
    private double longitude = 0.0;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    if (imageUri != null) {
                        imagePreview.setImageURI(imageUri);
                        imagePreview.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(), "Image selected!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_upload, container, false);

        etMushroomType = root.findViewById(R.id.etMushroomType);
        etDescription = root.findViewById(R.id.etDescription);
        spinnerCategory = root.findViewById(R.id.spinnerCategory);
        btnPickImage = root.findViewById(R.id.btnPickMedia); // same ID in layout
        btnGetLocation = root.findViewById(R.id.btnGetLocation);
        btnSubmit = root.findViewById(R.id.btnSubmit);
        imagePreview = root.findViewById(R.id.imagePreview);
        tvLatitude = root.findViewById(R.id.tvLatitude);
        tvLongitude = root.findViewById(R.id.tvLongitude);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Spinner options
        String[] categories = {"Edible", "Poisonous", "Inedible", "Medicinal"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        // Pick only images
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        // Get location
        btnGetLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            } else {
                getUserLocation();
            }
        });

        // Submit post
        btnSubmit.setOnClickListener(v -> {
            String mushroomType = etMushroomType.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();

            if (mushroomType.isEmpty() || description.isEmpty() || imageUri == null || userLocation.equals("Unknown")) {
                Toast.makeText(requireContext(), "Please fill all fields and attach an image.", Toast.LENGTH_SHORT).show();
                return;
            }

            MediaManager.get().upload(imageUri)
                    .unsigned("mycoscan")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Toast.makeText(requireContext(), "Uploading image...", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String cloudinaryUrl = resultData.get("secure_url").toString();

                            Map<String, Object> post = new HashMap<>();
                            post.put("mushroomType", mushroomType);
                            post.put("category", category);
                            post.put("description", description);
                            post.put("latitude", latitude);
                            post.put("longitude", longitude);
                            post.put("imageUrl", cloudinaryUrl);
                            post.put("timestamp", System.currentTimeMillis());
                            post.put("userId", getCurrentUserId());

                            FirebaseFirestore.getInstance()
                                    .collection("posts")
                                    .add(post)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(requireContext(), "Post saved!", Toast.LENGTH_SHORT).show();

                                        // Clear fields
                                        etMushroomType.setText("");
                                        etDescription.setText("");
                                        spinnerCategory.setSelection(0); // reset to first item
                                        imagePreview.setImageURI(null);
                                        imagePreview.setVisibility(View.GONE);
                                        imageUri = null;

                                        // Reset location
                                        tvLatitude.setText("Latitude: ");
                                        tvLongitude.setText("Longitude: ");
                                        latitude = 0.0;
                                        longitude = 0.0;
                                        userLocation = "Unknown";
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(), "Error saving post: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(requireContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        });

        return root;
    }

    // Handle location permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                userLocation = latitude + ", " + longitude;

                tvLatitude.setText("Latitude: " + latitude);
                tvLongitude.setText("Longitude: " + longitude);

                Toast.makeText(requireContext(), "Location captured!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Get current Firebase user ID
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "anonymous";
    }
}
