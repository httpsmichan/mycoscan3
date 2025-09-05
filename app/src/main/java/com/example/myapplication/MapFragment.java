package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.activity_map, container, false);

        mapView = root.findViewById(R.id.mapView);
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        drawDavaoBoundary();
        loadPostLocations();

        return root;
    }

    private void loadPostLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference postsRef = db.collection("posts");

        postsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                Double lat = doc.getDouble("latitude");
                Double lon = doc.getDouble("longitude");
                String type = doc.getString("mushroomType");
                String user = doc.getString("username");
                String imageUrl = doc.getString("imageUrl");

                // ✅ use helper method
                if (lat != null && lon != null && DavaoGeoFence.isInsideDavao(lat, lon)) {
                    Marker marker = new Marker(mapView);
                    marker.setPosition(new GeoPoint(lat, lon));
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    marker.setTitle((type != null ? type : "Post") +
                            (user != null ? "\nby " + user : ""));

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(requireContext())
                                .asBitmap()
                                .load(imageUrl)
                                .circleCrop()
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource,
                                                                @Nullable Transition<? super Bitmap> transition) {
                                        int size = 70;
                                        Bitmap smallBitmap = Bitmap.createScaledBitmap(resource, size, size, false);
                                        marker.setIcon(new BitmapDrawable(getResources(), smallBitmap));
                                        mapView.invalidate();
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                                });
                    }

                    mapView.getOverlays().add(marker);
                }
            }

            mapView.getController().setZoom(11.0);
            mapView.getController().setCenter(new GeoPoint(7.0731, 125.6088));
        });
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
