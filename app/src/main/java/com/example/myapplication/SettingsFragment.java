package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private TextView textUsername, textUserHandle, textUserBio, textFollowers, textFollowing, textPosts;
    private TextView btnEditProfile, btnSecurity, btnJournal, btnLogout, btnVerify;
    private Button btnAboutTab, btnPostsTab;
    private LinearLayout layoutAboutContent, layoutPostsContent;

    private RecyclerView recyclerUserPosts;
    private UserPostAdapter postAdapter;
    private List<Post> postList;
    private ImageView imageProfile, imageVerifiedBadge;

    private FirebaseFirestore db;
    private Uri selectedImageUri;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_settings_fragment, container, false);

        // Profile details
        imageProfile = view.findViewById(R.id.imageProfile);
        textUsername = view.findViewById(R.id.textUsername);
        textUserHandle = view.findViewById(R.id.textUserHandle);
        textUserBio = view.findViewById(R.id.textUserBio);
        textFollowers = view.findViewById(R.id.textFollowers);
        textFollowing = view.findViewById(R.id.textFollowing);
        textPosts = view.findViewById(R.id.textPosts);
        imageVerifiedBadge = view.findViewById(R.id.imageVerifiedBadge);

        // Tab buttons and content containers
        btnAboutTab = view.findViewById(R.id.btnAboutTab);
        btnPostsTab = view.findViewById(R.id.btnPostsTab);
        layoutAboutContent = view.findViewById(R.id.layoutAboutContent);
        layoutPostsContent = view.findViewById(R.id.layoutPostsContent);

        // Action buttons
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSecurity = view.findViewById(R.id.btnSecurity);
        btnJournal = view.findViewById(R.id.btnJournal);
        btnVerify = view.findViewById(R.id.btnVerify);
        btnLogout = view.findViewById(R.id.btnLogout);

        // RecyclerView for posts
        recyclerUserPosts = view.findViewById(R.id.recyclerUserPosts);
        recyclerUserPosts.setLayoutManager(new GridLayoutManager(getContext(), 3));

        postList = new ArrayList<>();
        postAdapter = new UserPostAdapter(getContext(), postList);
        recyclerUserPosts.setAdapter(postAdapter);


        db = FirebaseFirestore.getInstance();

        loadUserInfoAndPosts();

        // Click listeners
        imageProfile.setOnClickListener(v -> openImagePicker());
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
        btnSecurity.setOnClickListener(v -> startActivity(new Intent(getActivity(), SecurityActivity.class)));
        btnJournal.setOnClickListener(v -> startActivity(new Intent(getActivity(), JournalActivity.class)));

        // ✅ Updated: redirect to Verification.java
        btnVerify.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Verification.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        // Tab switching logic
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

        return view;
    }

    private void loadUserInfoAndPosts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            String username = documentSnapshot.getString("username");
                            Long followersCount = documentSnapshot.getLong("followers");
                            Long followingCount = documentSnapshot.getLong("following");
                            String bio = documentSnapshot.getString("bio");

                            if (fullName != null && !fullName.isEmpty()) {
                                textUsername.setText(fullName);
                            } else {
                                textUsername.setText("User");
                            }

                            if (username != null && !username.isEmpty()) {
                                textUserHandle.setText("@" + username);
                            } else {
                                textUserHandle.setText("@unknown");
                            }

                            // ✅ Followers and Following
                            if (followersCount != null) {
                                textFollowers.setText(String.valueOf(followersCount));
                            } else {
                                textFollowers.setText("0");
                            }

                            if (followingCount != null) {
                                textFollowing.setText(String.valueOf(followingCount));
                            } else {
                                textFollowing.setText("0");
                            }

                            if (bio != null && !bio.isEmpty()) {
                                textUserBio.setText(bio);
                            } else {
                                textUserBio.setText("No bio yet.");
                            }

                            // 🔑 Check applications collection for verification
                            db.collection("applications")
                                    .whereEqualTo("userId", user.getUid())
                                    .get()
                                    .addOnSuccessListener(query -> {
                                        if (!query.isEmpty()) {
                                            String status = query.getDocuments().get(0).getString("status");
                                            if ("approved".equalsIgnoreCase(status)) {
                                                imageVerifiedBadge.setVisibility(View.VISIBLE);
                                            } else {
                                                imageVerifiedBadge.setVisibility(View.GONE);
                                            }
                                        }
                                    });

                            // Profile photo
                            String photoUrl = documentSnapshot.getString("profilePhoto");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_person_placeholder)
                                        .error(R.drawable.ic_person_placeholder)
                                        .circleCrop()
                                        .into(imageProfile);
                            } else {
                                imageProfile.setImageResource(R.drawable.ic_person_placeholder);
                            }
                        }
                    });

            loadUserPosts(user.getUid());
        } else {
            textUsername.setText("No user signed in");
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(imageProfile);

            uploadImageToCloudinary(selectedImageUri);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;

        MediaManager.get().upload(imageUri)
                .option("folder", "profile_photos")
                .callback(new UploadCallback() {
                    @Override public void onStart(String requestId) { }
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) { }
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String cloudinaryUrl = (String) resultData.get("secure_url");
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            db.collection("users").document(user.getUid())
                                    .update("profilePhoto", cloudinaryUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show();
                                        db.collection("logs").add(new LogEntry(
                                                user.getUid(),
                                                "uploaded a profile photo",
                                                System.currentTimeMillis()
                                        ));
                                        Log.d("SettingsFragment", "User " + user.getUid() + " uploaded a profile photo");
                                    })
                                    .addOnFailureListener(e -> Log.e("SettingsFragment", "Failed to update profile photo", e));
                        }
                    }
                    @Override public void onError(String requestId, ErrorInfo error) {
                        Log.e("SettingsFragment", "Cloudinary upload error: " + error.getDescription());
                        Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w("SettingsFragment", "Cloudinary upload rescheduled: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    private void loadUserPosts(String userId) {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w("SettingsFragment", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        postList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Post post = doc.toObject(Post.class);
                            post.setPostId(doc.getId());
                            postList.add(post);
                            Log.d("SettingsFragment", "Loaded user post ID: " + doc.getId());
                        }
                        // Sort by timestamp (latest first)
                        Collections.sort(postList, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                        postAdapter.notifyDataSetChanged();
                        textPosts.setText(String.valueOf(postList.size())); // Update post count
                        Log.d("SettingsFragment", "Loaded " + postList.size() + " posts");
                    }
                });
    }

    public static class LogEntry {
        private String userId;
        private String action;
        private long timestamp;

        public LogEntry() { }
        public LogEntry(String userId, String action, long timestamp) {
            this.userId = userId;
            this.action = action;
            this.timestamp = timestamp;
        }

        public String getUserId() { return userId; }
        public String getAction() { return action; }
        public long getTimestamp() { return timestamp; }
    }
}
