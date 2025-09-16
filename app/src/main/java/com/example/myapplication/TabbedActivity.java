package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class TabbedActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private boolean isVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            checkUserStatusAndLoad(); // 🔎 Firestore check once at startup
        }

        // Check user active/inactive status
        updateUserStatus();

        // Set up bottom navigation listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // ✅ Use cached verified status
                selectedFragment = isVerified ? new MycologistsHome() : new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_favorites) {
                selectedFragment = new FavoritesFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new UploadFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void checkUserStatusAndLoad() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            loadFragment(new HomeFragment());
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object verifiedObj = documentSnapshot.get("verified");

                        Log.d("TabbedActivity", "Doc path: users/" + currentUser.getUid());
                        Log.d("TabbedActivity", "verified field raw value: " + verifiedObj);

                        boolean verified = false;
                        if (verifiedObj instanceof Boolean) {
                            verified = (Boolean) verifiedObj;
                        } else if (verifiedObj instanceof String) {
                            verified = Boolean.parseBoolean(((String) verifiedObj).toLowerCase());
                        }

                        isVerified = verified; // ✅ cache result

                        if (isVerified) {
                            Log.d("TabbedActivity", "✅ Loading MycologistsHome");
                            loadFragment(new MycologistsHome());
                        } else {
                            Log.d("TabbedActivity", "❌ verified not true -> Loading HomeFragment");
                            loadFragment(new HomeFragment());
                        }
                    } else {
                        Log.d("TabbedActivity", "❌ User doc not found");
                        loadFragment(new HomeFragment());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TabbedActivity", "Firestore error: ", e);
                    loadFragment(new HomeFragment());
                });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void updateUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Calculate start of today
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        // ✅ Always update lastLogin when the app is opened
        long now = System.currentTimeMillis();
        userRef.update("lastLogin", now);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Long lastLogin = documentSnapshot.getLong("lastLogin");
                if (lastLogin != null && lastLogin >= startOfDay) {
                    userRef.update("status", "Active");
                } else {
                    userRef.update("status", "Inactive");
                }
            }
        });
    }
}
