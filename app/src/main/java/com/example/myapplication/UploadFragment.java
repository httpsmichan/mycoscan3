package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
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
import java.util.List;
import java.util.Map;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.api.IMapController;

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
    private MapView mapPreview;

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
        btnPickImage = root.findViewById(R.id.btnPickMedia);
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

        // Map preview setup
        mapPreview = root.findViewById(R.id.mapPreview);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapPreview.setTileSource(TileSourceFactory.MAPNIK);
        mapPreview.setMultiTouchControls(true);

        // Submit post
        btnSubmit.setOnClickListener(v -> {
            String mushroomType = etMushroomType.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();

            if (!DavaoGeoFence.isInsideDavao(latitude, longitude)) {
                Toast.makeText(requireContext(), "You're outside Davao City. Posting is only allowed inside Davao City.", Toast.LENGTH_LONG).show();
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

                            // Fetch username first
                            getCurrentUsername(username -> {
                                // Convert latitude/longitude to address
                                final String[] addressHolder = { "Unknown location" };
                                try {
                                    Geocoder geocoder = new Geocoder(requireContext());
                                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        addressHolder[0] = addresses.get(0).getAddressLine(0);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // Prepare post data
                                Map<String, Object> post = new HashMap<>();
                                post.put("mushroomType", mushroomType);
                                post.put("category", category);
                                post.put("description", description);
                                post.put("latitude", latitude);
                                post.put("longitude", longitude);
                                post.put("location", addressHolder[0]);
                                post.put("imageUrl", cloudinaryUrl);
                                post.put("timestamp", System.currentTimeMillis());
                                post.put("userId", getCurrentUserId());
                                post.put("username", username);
                                post.put("verified", "not verified");

                                // Save to Firestore
                                FirebaseFirestore.getInstance()
                                        .collection("posts")
                                        .add(post)
                                        .addOnSuccessListener(documentReference -> {
                                            String postId = documentReference.getId();
                                            documentReference.update("postId", postId);

                                            Toast.makeText(requireContext(), "Post saved!", Toast.LENGTH_SHORT).show();

                                            // Reset fields
                                            etMushroomType.setText("");
                                            etDescription.setText("");
                                            spinnerCategory.setSelection(0);
                                            imagePreview.setImageURI(null);
                                            imagePreview.setVisibility(View.GONE);
                                            imageUri = null;

                                            tvLatitude.setText("Latitude: ");
                                            tvLongitude.setText("Longitude: ");
                                            latitude = 0.0;
                                            longitude = 0.0;
                                            userLocation = "Unknown";

                                            mapPreview.getOverlays().clear();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(requireContext(), "Error saving post: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );

                            });
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

                mapPreview.setVisibility(View.VISIBLE);
                IMapController mapController = mapPreview.getController();
                mapController.setZoom(15.0);
                GeoPoint point = new GeoPoint(latitude, longitude);
                mapController.setCenter(point);

                mapPreview.getOverlays().clear();
                Marker marker = new Marker(mapPreview);
                marker.setPosition(point);
                marker.setTitle("Exact Location");
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapPreview.getOverlays().add(marker);

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

    // Fetch username from Firestore users collection
    private void getCurrentUsername(OnUsernameFetchedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            listener.onUsernameFetched(username != null ? username : "anonymous");
                        } else {
                            listener.onUsernameFetched("anonymous");
                        }
                    })
                    .addOnFailureListener(e -> listener.onUsernameFetched("anonymous"));
        } else {
            listener.onUsernameFetched("anonymous");
        }
    }

    // Simple callback for async username fetch
    interface OnUsernameFetchedListener {
        void onUsernameFetched(String username);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) {
            mapPreview.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapPreview != null) {
            mapPreview.onPause();
        }
    }
}
