package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    private Button btnFollow;
    private Button btnAboutTab, btnPostsTab;
    private View layoutAboutContent, layoutPostsContent;

    private TextView tvUserHandle, tvUsername, tvFollowersCount, tvFollowingCount, tvPostsCount, tvBio;

    private ImageView ivProfilePhoto;
    private RecyclerView recyclerUserPosts;
    private FirebaseFirestore db;
    private String visitedUserId;  // the user whose profile is being viewed
    private String visitedUsername; // the username being viewed
    private String currentUserId;  // the logged-in user
    private boolean isFollowing = false; // track state
    private UserPostsGridAdapter userPostsAdapter;
    private final List<Post> userPostsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get data from Intent
        visitedUserId = getIntent().getStringExtra("visitedUserId");
        visitedUsername = getIntent().getStringExtra("visitedUsername");

        // Initialize views
        initViews();

        // RecyclerView setup
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        recyclerUserPosts.setLayoutManager(new GridLayoutManager(this, 3));
        userPostsAdapter = new UserPostsGridAdapter(this, userPostsList); // Changed from PostAdapter
        recyclerUserPosts.setAdapter(userPostsAdapter);

        // Load user profile data
        loadUserProfile();

        // Check if current user is following this user
        checkFollowStatus();

        // Load user's posts
        loadUserPosts(visitedUserId);

        // Set follow button click listener
        btnFollow.setOnClickListener(v -> toggleFollow());

        btnAboutTab.setOnClickListener(v -> {
            layoutAboutContent.setVisibility(View.VISIBLE);
            layoutPostsContent.setVisibility(View.GONE);
            btnAboutTab.setBackgroundResource(R.drawable.tab_selected_background);
            btnPostsTab.setBackgroundResource(R.drawable.tab_unselected_background);
        });

        btnPostsTab.setOnClickListener(v -> {
            layoutAboutContent.setVisibility(View.GONE);
            layoutPostsContent.setVisibility(View.VISIBLE);
            btnPostsTab.setBackgroundResource(R.drawable.tab_selected_background);
            btnAboutTab.setBackgroundResource(R.drawable.tab_unselected_background);
        });

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1; // Each post takes 1 span (1/3 of the width)
            }
        });
        recyclerUserPosts.setLayoutManager(gridLayoutManager);
    }

    private void addItemDecoration() {
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing); // Add this dimen to your resources
        recyclerUserPosts.addItemDecoration(new GridSpacingItemDecoration(3, spacing, true));
    }

    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, android.view.View view,
                                   RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }

    private void initViews() {
        btnFollow = findViewById(R.id.btnFollow);
        tvUserHandle = findViewById(R.id.textUserHandle);
        tvUsername = findViewById(R.id.textUsername);
        tvFollowersCount = findViewById(R.id.textFollowers);
        tvFollowingCount = findViewById(R.id.textFollowing);
        tvPostsCount = findViewById(R.id.textPosts);
        tvBio = findViewById(R.id.textUserBio);
        ivProfilePhoto = findViewById(R.id.imageProfile);
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        btnAboutTab = findViewById(R.id.btnAboutTab);
        btnPostsTab = findViewById(R.id.btnPostsTab);
        layoutAboutContent = findViewById(R.id.layoutAboutContent);
        layoutPostsContent = findViewById(R.id.layoutPostsContent);

        // Hide follow button if viewing own profile
        if (visitedUserId != null && visitedUserId.equals(currentUserId)) {
            btnFollow.setVisibility(View.GONE);
        }
    }

    private void loadUserProfile() {
        if (visitedUserId == null) {
            // If we only have username, find the user ID first
            if (visitedUsername != null) {
                findUserByUsername(visitedUsername);
            } else {
                Toast.makeText(this, "Error: No user specified", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }

        db.collection("users").document(visitedUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        populateUserProfile(document);
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error loading user profile", e);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void findUserByUsername(String username) {
        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        visitedUserId = document.getId();
                        populateUserProfile(document);
                        checkFollowStatus();
                        loadUserPosts(visitedUserId);

                        // Show follow button now that we have the user ID
                        if (!visitedUserId.equals(currentUserId)) {
                            btnFollow.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfile", "Error finding user by username", e);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void populateUserProfile(DocumentSnapshot document) {
        // Username (handle)
        String username = document.getString("username");
        if (username != null) {
            tvUserHandle.setText(username);
            visitedUsername = username;
        }

        // Full name
        String fullName = document.getString("fullName");
        if (fullName != null) {
            tvUsername.setText(fullName);
        }

        // Bio
        String bio = document.getString("bio");
        if (bio != null && !bio.isEmpty()) {
            tvBio.setText(bio);
            tvBio.setVisibility(View.VISIBLE);
        } else {
            tvBio.setVisibility(View.GONE);
        }

        // Profile photo
        String profilePhoto = document.getString("profilePhoto");
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            Glide.with(this)
                    .load(profilePhoto)
                    .placeholder(R.drawable.ic_settings)
                    .circleCrop()
                    .into(ivProfilePhoto);
        }

        // Counts
        Long followers = document.getLong("followers");
        Long following = document.getLong("following");

        tvFollowersCount.setText(String.valueOf(followers != null ? followers : 0));
        tvFollowingCount.setText(String.valueOf(following != null ? following : 0));
    }

    private void checkFollowStatus() {
        if (visitedUserId == null || currentUserId == null || visitedUserId.equals(currentUserId)) {
            return;
        }

        // Check if current user is in the visited user's followers list
        db.collection("users")
                .document(visitedUserId)
                .collection("followersList")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    isFollowing = document.exists();
                    updateFollowButtonText();
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Error checking follow status", e));
    }

    private void loadUserPosts(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("UserProfile", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        userPostsList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId());
                            userPostsList.add(post);
                            Log.d("UserProfile", "Loaded user post ID: " + doc.getId());
                        }
                        // Sort by timestamp (latest first)
                        userPostsList.sort((p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                        userPostsAdapter.notifyDataSetChanged();
                        tvPostsCount.setText(String.valueOf(userPostsList.size())); // update post count
                        Log.d("UserProfile", "Loaded " + userPostsList.size() + " posts");
                    }
                });
    }


    private void toggleFollow() {
        if (visitedUserId == null || currentUserId == null) {
            Toast.makeText(this, "Error: Missing user info", Toast.LENGTH_SHORT).show();
            return;
        }

        if (visitedUserId.equals(currentUserId)) {
            Toast.makeText(this, "Cannot follow yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple clicks
        btnFollow.setEnabled(false);

        DocumentReference visitedUserRef = db.collection("users").document(visitedUserId);
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);

        if (!isFollowing) {
            // FOLLOW
            Map<String, Object> followerData = new HashMap<>();
            followerData.put("userId", currentUserId);
            followerData.put("timestamp", System.currentTimeMillis());

            Map<String, Object> followingData = new HashMap<>();
            followingData.put("userId", visitedUserId);
            followingData.put("timestamp", System.currentTimeMillis());

            // Add to followers/following lists
            visitedUserRef.collection("followersList").document(currentUserId).set(followerData);
            currentUserRef.collection("followingList").document(visitedUserId).set(followingData);

            // Increment counters
            visitedUserRef.update("followers", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {
                        currentUserRef.update("following", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid1 -> {
                                    isFollowing = true;
                                    updateFollowButtonText();
                                    updateFollowersCount(1);
                                    btnFollow.setEnabled(true);
                                    Toast.makeText(this, "Now following " + visitedUsername, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserProfile", "Error updating following count", e);
                                    btnFollow.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UserProfile", "Error updating followers count", e);
                        btnFollow.setEnabled(true);
                    });

        } else {
            // UNFOLLOW
            // Remove from followers/following lists
            visitedUserRef.collection("followersList").document(currentUserId).delete();
            currentUserRef.collection("followingList").document(visitedUserId).delete();

            // Decrement counters
            visitedUserRef.update("followers", FieldValue.increment(-1))
                    .addOnSuccessListener(aVoid -> {
                        currentUserRef.update("following", FieldValue.increment(-1))
                                .addOnSuccessListener(aVoid1 -> {
                                    isFollowing = false;
                                    updateFollowButtonText();
                                    updateFollowersCount(-1);
                                    btnFollow.setEnabled(true);
                                    Toast.makeText(this, "Unfollowed " + visitedUsername, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("UserProfile", "Error updating following count", e);
                                    btnFollow.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UserProfile", "Error updating followers count", e);
                        btnFollow.setEnabled(true);
                    });
        }
    }

    private void updateFollowButtonText() {
        if (btnFollow != null) {
            btnFollow.setText(isFollowing ? "Unfollow" : "Follow");
        }
    }

    private void updateFollowersCount(int change) {
        int currentCount = Integer.parseInt(tvFollowersCount.getText().toString());
        tvFollowersCount.setText(String.valueOf(currentCount + change));
    }


}