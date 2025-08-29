package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

public class MapFragment extends Fragment {

    private MapView mapView;

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

        loadPostLocations();

        return root;
    }

    private void loadPostLocations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference postsRef = db.collection("posts");

        postsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();

            for (DocumentSnapshot doc : docs) {
                Double lat = doc.getDouble("latitude");
                Double lon = doc.getDouble("longitude");
                String type = doc.getString("mushroomType");
                String user = doc.getString("username");

                if (lat != null && lon != null) {
                    GeoPoint point = new GeoPoint(lat, lon);
                    Marker marker = new Marker(mapView);
                    marker.setPosition(point);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle((type != null ? type : "Post") +
                            (user != null ? "\nby " + user : ""));
                    mapView.getOverlays().add(marker);
                }
            }

            // Center on first marker if exists
            if (!docs.isEmpty()) {
                Double lat = docs.get(0).getDouble("latitude");
                Double lon = docs.get(0).getDouble("longitude");
                if (lat != null && lon != null) {
                    mapView.getController().setZoom(10.0);
                    mapView.getController().setCenter(new GeoPoint(lat, lon));
                }
            }
        });
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
