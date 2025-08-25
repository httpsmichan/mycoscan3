package com.example.myapplication;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class ImageDatabase extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Cloudinary configuration
        Map config = new HashMap();
        config.put("cloud_name", "diaw4uoea");

        // Initialize Cloudinary
        MediaManager.init(this, config);
    }
}
