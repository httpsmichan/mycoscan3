package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TabbedActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView mushroomRecyclerView;
    private MushroomAdapter mushroomAdapter;
    private List<String> mushroomList = new ArrayList<>();
    private boolean isVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabbed);

        // Bottom navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // ✅ Setup Navigation Drawer RecyclerView
        navigationView = findViewById(R.id.nav_view);
        mushroomRecyclerView = navigationView.findViewById(R.id.mushroomRecyclerView);
        mushroomRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ✅ Setup adapter for RecyclerView
        mushroomAdapter = new MushroomAdapter(mushroomList, mushroomName -> {
            Intent intent = new Intent(TabbedActivity.this, EncyclopediaActivity.class);
            intent.putExtra("mushroomName", mushroomName);
            startActivity(intent);
        });

        mushroomRecyclerView.setAdapter(mushroomAdapter);

        // ✅ Load mushrooms from Firestore
        loadMushroomsFromFirestore();

        // Handle incoming intent (upload flow)
        if (getIntent().getBooleanExtra("openUploadTab", false)) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);

            UploadFragment uploadFragment = new UploadFragment();
            Bundle bundle = new Bundle();
            bundle.putString("mushroomType", getIntent().getStringExtra("mushroomType"));
            bundle.putString("category", getIntent().getStringExtra("category"));
            bundle.putString("description", getIntent().getStringExtra("description"));
            bundle.putString("photoUri", getIntent().getStringExtra("photoUri"));
            bundle.putDouble("latitude", getIntent().getDoubleExtra("latitude", 0.0));
            bundle.putDouble("longitude", getIntent().getDoubleExtra("longitude", 0.0));
            uploadFragment.setArguments(bundle);

            loadFragment(uploadFragment);
        } else if (savedInstanceState == null) {
            checkUserStatusAndLoad();
        }

        // Track user status
        updateUserStatus();

        // ✅ Bottom navigation listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
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

    private void loadMushroomsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("mushroom-encyclopedia")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    mushroomList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("mushroomName");
                        if (name != null) {
                            mushroomList.add(name);
                        }
                    }
                    mushroomAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Drawer", "Error loading mushrooms", e);
                    Toast.makeText(this, "Failed to load mushrooms", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.getBooleanExtra("openUploadTab", false)) {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);

            UploadFragment uploadFragment = new UploadFragment();
            Bundle bundle = new Bundle();
            bundle.putString("mushroomType", intent.getStringExtra("mushroomType"));
            bundle.putString("category", intent.getStringExtra("category"));
            bundle.putString("description", intent.getStringExtra("description"));
            bundle.putString("photoUri", intent.getStringExtra("photoUri"));
            bundle.putDouble("latitude", intent.getDoubleExtra("latitude", 0.0));
            bundle.putDouble("longitude", intent.getDoubleExtra("longitude", 0.0));
            uploadFragment.setArguments(bundle);

            loadFragment(uploadFragment);

            intent.removeExtra("openUploadTab");
        }
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

                        boolean verified = false;
                        if (verifiedObj instanceof Boolean) {
                            verified = (Boolean) verifiedObj;
                        } else if (verifiedObj instanceof String) {
                            verified = Boolean.parseBoolean(((String) verifiedObj).toLowerCase());
                        }

                        isVerified = verified;

                        if (isVerified) {
                            loadFragment(new MycologistsHome());
                        } else {
                            loadFragment(new HomeFragment());
                        }
                    } else {
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

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

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
