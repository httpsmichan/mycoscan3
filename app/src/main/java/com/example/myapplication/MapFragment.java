package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView mapView;
    private Polygon davaoPolygon;
    private EditText searchBar;
    private Button filterAll, filterEdible, filterPoisonous, filterUnknown, filterByUser, filterInedible;

    private List<DocumentSnapshot> allPosts = new ArrayList<>();
    private String currentFilter = "All";
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_map, container, false);

        mapView = root.findViewById(R.id.mapView);
        searchBar = root.findViewById(R.id.searchBar);
        filterAll = root.findViewById(R.id.filterAll);
        filterEdible = root.findViewById(R.id.filterEdible);
        filterPoisonous = root.findViewById(R.id.filterPoisonous);
        filterUnknown = root.findViewById(R.id.filterUnknown);
        filterByUser = root.findViewById(R.id.filterByUser);
        filterInedible = root.findViewById(R.id.filterInedible);


        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        drawDavaoBoundary();
        setupFilters();
        setupSearch();
        loadPostLocations();

        return root;
    }

    private void setupFilters() {
        filterAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterButtons();
            applyFilters();
        });

        filterEdible.setOnClickListener(v -> {
            currentFilter = "Edible";
            updateFilterButtons();
            applyFilters();
        });

        filterInedible.setOnClickListener(v -> {
            currentFilter = "Inedible";
            updateFilterButtons();
            applyFilters();
        });

        filterPoisonous.setOnClickListener(v -> {
            currentFilter = "Poisonous";
            updateFilterButtons();
            applyFilters();
        });

        filterUnknown.setOnClickListener(v -> {
            currentFilter = "Unknown / Needs ID";
            updateFilterButtons();
            applyFilters();
        });

        filterByUser.setOnClickListener(v -> {
            showUserFilterDialog();
        });

        updateFilterButtons();
    }

    private void updateFilterButtons() {
        // Reset all buttons
        filterAll.setBackgroundColor(Color.LTGRAY);
        filterEdible.setBackgroundColor(Color.LTGRAY);
        filterPoisonous.setBackgroundColor(Color.LTGRAY);
        filterUnknown.setBackgroundColor(Color.LTGRAY);
        filterByUser.setBackgroundColor(Color.LTGRAY);
        filterInedible.setBackgroundColor(Color.LTGRAY);

        // Highlight selected filter
        if (currentFilter.equals("All")) {
            filterAll.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if (currentFilter.equals("Edible")) {
            filterEdible.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if (currentFilter.equals("Inedible")) {
            filterInedible.setBackgroundColor(Color.parseColor("#FF9800")); // orange, for example
        }else if (currentFilter.equals("Poisonous")) {
            filterPoisonous.setBackgroundColor(Color.parseColor("#F44336"));
        } else if (currentFilter.equals("Unknown / Needs ID")) {
            filterUnknown.setBackgroundColor(Color.parseColor("#9E9E9E"));
        } else {
            filterByUser.setBackgroundColor(Color.parseColor("#2196F3"));
        }
    }

    private void showUserFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Filter by Username");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter username");
        builder.setView(input);

        builder.setPositiveButton("Filter", (dialog, which) -> {
            String username = input.getText().toString().trim();
            if (!username.isEmpty()) {
                currentFilter = "User:" + username;
                updateFilterButtons();
                applyFilters();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadPostLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference postsRef = db.collection("posts");

        postsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            allPosts.clear();
            allPosts.addAll(queryDocumentSnapshots.getDocuments());
            applyFilters();

            mapView.getController().setZoom(11.0);
            mapView.getController().setCenter(new GeoPoint(7.0731, 125.6088));
        });
    }

    private void applyFilters() {
        if (!isAdded() || mapView == null) return; // fragment not attached, do nothing

        mapView.getOverlays().clear();
        mapView.getOverlays().add(davaoPolygon);

        for (DocumentSnapshot doc : allPosts) {
            Double lat = doc.getDouble("latitude");
            Double lon = doc.getDouble("longitude");
            if (lat == null || lon == null || !DavaoGeoFence.isInsideDavao(lat, lon)) continue;

            String type = doc.getString("mushroomType");
            String user = doc.getString("username");
            String category = doc.getString("category");
            String imageUrl = doc.getString("imageUrl");
            String postId = doc.getId();

            // filters + search checks...
            if (!currentFilter.equals("All")) {
                if (currentFilter.startsWith("User:")) {
                    String filterUsername = currentFilter.substring(5);
                    if (user == null || !user.equalsIgnoreCase(filterUsername)) continue;
                } else if (category == null || !category.equals(currentFilter)) continue;
            }
            if (!searchQuery.isEmpty()) {
                boolean matchFound = false;
                if (type != null && type.toLowerCase().contains(searchQuery)) matchFound = true;
                if (user != null && user.toLowerCase().contains(searchQuery)) matchFound = true;
                if (!matchFound) continue;
            }

            // 🔒 Check again before creating marker
            if (mapView == null) return;

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lon));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            marker.setTitle((type != null ? type : "Post") + (user != null ? "\nby " + user : ""));
            marker.setRelatedObject(postId);

            marker.setOnMarkerClickListener((m, mv) -> {
                showPostDetails(doc);
                return true;
            });

            // Glide safe load (with isAdded() check already added earlier)
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .asBitmap()
                        .load(imageUrl)
                        .circleCrop()
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource,
                                                        @Nullable Transition<? super Bitmap> transition) {
                                if (!isAdded() || mapView == null) return;
                                int size = 70;
                                Bitmap smallBitmap = Bitmap.createScaledBitmap(resource, size, size, false);
                                marker.setIcon(new BitmapDrawable(requireContext().getResources(), smallBitmap));
                                mapView.invalidate();
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {}
                        });
            }

            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }

    private void showPostDetails(DocumentSnapshot doc) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_post_details, null);
        bottomSheet.setContentView(sheetView);

        ImageView postImage = sheetView.findViewById(R.id.postDetailImage);
        TextView mushroomType = sheetView.findViewById(R.id.postDetailMushroomType);
        TextView category = sheetView.findViewById(R.id.postDetailCategory);
        TextView username = sheetView.findViewById(R.id.postDetailUsername);
        TextView verifiedBadge = sheetView.findViewById(R.id.postDetailVerified);
        TextView description = sheetView.findViewById(R.id.postDetailDescription);
        TextView location = sheetView.findViewById(R.id.postDetailLocation);
        TextView reportBtn = sheetView.findViewById(R.id.btnReportPost);

        String imageUrl = doc.getString("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) Glide.with(requireContext()).load(imageUrl).into(postImage);

        mushroomType.setText(doc.getString("mushroomType"));

        String cat = doc.getString("category");
        if (cat != null) {
            category.setText(cat);
            category.setVisibility(View.VISIBLE);
            if (cat.equals("Edible")) category.setBackgroundColor(Color.parseColor("#4CAF50"));
            else if (cat.equals("Poisonous")) category.setBackgroundColor(Color.parseColor("#F44336"));
            else if (cat.equals("Unknown / Needs ID")) category.setBackgroundColor(Color.parseColor("#9E9E9E"));
        } else category.setVisibility(View.GONE);

        username.setText(doc.getString("username"));

        String verified = doc.getString("verified");
        verifiedBadge.setVisibility(verified != null && verified.equals("verified") ? View.VISIBLE : View.GONE);

        String desc = doc.getString("description");
        if (desc != null && !desc.isEmpty()) {
            description.setText(desc);
            description.setVisibility(View.VISIBLE);
        } else description.setVisibility(View.GONE);

        String loc = doc.getString("location");
        if (loc != null) location.setText("📍 " + loc);

        // Report button click: open ReportActivity
        reportBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ReportActivity.class);
            intent.putExtra("postId", doc.getId()); // pass post ID if needed
            startActivity(intent);
            bottomSheet.dismiss();
        });


        bottomSheet.show();
    }

    private void reportPost(String postId) {
        if (postId == null) {
            Toast.makeText(requireContext(), "Cannot report this post", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Report Post");
        builder.setMessage("Why are you reporting this post?");

        String[] reasons = {"Incorrect Information", "Inappropriate Content", "Spam", "Other"};
        builder.setItems(reasons, (dialog, which) -> {
            String reason = reasons[which];
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            java.util.Map<String, Object> report = new java.util.HashMap<>();
            report.put("postId", postId);
            report.put("reason", reason);
            report.put("timestamp", System.currentTimeMillis());
            report.put("reportedBy", com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid());

            db.collection("reports")
                    .add(report)
                    .addOnSuccessListener(docRef ->
                            Toast.makeText(requireContext(), "Report submitted. Thank you!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Failed to submit report", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void drawDavaoBoundary() {
        davaoPolygon = new Polygon();
        davaoPolygon.setPoints(DavaoGeoFence.getBoundary());
        davaoPolygon.setStrokeColor(Color.BLUE);
        davaoPolygon.setFillColor(Color.TRANSPARENT);
        davaoPolygon.setStrokeWidth(2f);
        mapView.getOverlays().add(davaoPolygon);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
