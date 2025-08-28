package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class PostDetailActivity extends AppCompatActivity {

    private MapView miniMapView;
    private double latitude, longitude;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_post_detail);

        ImageView ivDetailImage = findViewById(R.id.ivDetailImage);
        TextView tvDetailType = findViewById(R.id.tvDetailType);
        TextView tvDetailDesc = findViewById(R.id.tvDetailDesc);
        TextView tvDetailUser = findViewById(R.id.tvDetailUser);
        miniMapView = findViewById(R.id.miniMapView);
        Button btnOpenFullMap = findViewById(R.id.btnOpenFullMap);

        // Get data passed
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String mushroomType = getIntent().getStringExtra("mushroomType");
        String description = getIntent().getStringExtra("description");
        String username = getIntent().getStringExtra("username");
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Set values
        tvDetailType.setText(mushroomType);
        tvDetailDesc.setText(description);
        tvDetailUser.setText("Posted by: " + (username != null ? username : "Unknown"));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(ivDetailImage);
        } else {
            ivDetailImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Setup mini OSM preview
        miniMapView.setMultiTouchControls(false); // disable zoom/scroll
        miniMapView.setClickable(false);
        miniMapView.getController().setZoom(15.0);

        GeoPoint postLocation = new GeoPoint(latitude, longitude);
        miniMapView.getController().setCenter(postLocation);

        Marker marker = new Marker(miniMapView);
        marker.setPosition(postLocation);
        marker.setTitle("Post Location");
        miniMapView.getOverlays().add(marker);

        // Click → open full screen MapActivity
        miniMapView.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailActivity.this, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startActivity(intent);
        });

        btnOpenFullMap.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailActivity.this, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        miniMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        miniMapView.onPause();
    }
}
