package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private MapView miniMapView;
    private double latitude, longitude;

    private RecyclerView rvComments;
    private EditText etComment;
    private Button btnPostComment;
    private List<Comment> commentList;
    private CommentAdapter commentAdapter;
    private FirebaseFirestore db;

    private String username;
    private String postId;
    private Button btnUpvote, btnDownvote;
    private TextView tvScore;

    private String currentUserId;
    private String currentUserVote = null;
    private TextView tvUpvotes, tvDownvotes;


    private void getCurrentUsername(OnUsernameFetchedListener listener) {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            listener.onUsernameFetched("Anonymous");
            return;
        }

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String uname = documentSnapshot.getString("username");
                        listener.onUsernameFetched(uname != null ? uname : "Anonymous");
                    } else {
                        listener.onUsernameFetched("Anonymous");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PostDetailActivity", "Error fetching username", e);
                    listener.onUsernameFetched("Anonymous");
                });
    }

    interface OnUsernameFetchedListener {
        void onUsernameFetched(String username);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_post_detail);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Error loading post details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String imageUrl = intent.getStringExtra("imageUrl");
        String mushroomType = intent.getStringExtra("mushroomType");
        String description = intent.getStringExtra("description");
        username = intent.getStringExtra("username");
        postId = intent.getStringExtra("postId");
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        if (postId == null || postId.isEmpty()) {
            Toast.makeText(this, "Post ID missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageView ivDetailImage = findViewById(R.id.ivDetailImage);
        TextView tvDetailType = findViewById(R.id.tvDetailType);
        TextView tvDetailDesc = findViewById(R.id.tvDetailDesc);
        TextView tvDetailUser = findViewById(R.id.tvDetailUser);
        miniMapView = findViewById(R.id.miniMapView);
        btnUpvote = findViewById(R.id.btnUpvote);
        btnDownvote = findViewById(R.id.btnDownvote);
        tvUpvotes = findViewById(R.id.tvUpvotes);
        tvDownvotes = findViewById(R.id.tvDownvotes);


        tvDetailType.setText(mushroomType != null ? mushroomType : "Unknown Type");
        tvDetailDesc.setText(description != null ? description : "No description available");
        tvDetailUser.setText("Posted by: " + (username != null ? username : "Unknown"));

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(ivDetailImage);
        }

        loadVotes();
        setupMiniMap();
        setupComments();

        btnUpvote.setOnClickListener(v -> updateVote("upvote"));
        btnDownvote.setOnClickListener(v -> updateVote("downvote"));
    }

    private void loadVotes() {
        db.collection("posts").document(postId)
                .collection("votes")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null) {
                        int upvotes = 0;
                        int downvotes = 0;

                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot) {
                            String type = doc.getString("type");
                            if ("upvote".equals(type)) upvotes++;
                            else if ("downvote".equals(type)) downvotes++;
                        }

                        tvUpvotes.setText("+" + upvotes);
                        tvDownvotes.setText("-" + downvotes);

                        if (currentUserId != null) {
                            currentUserVote = null;
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                                if (doc.getId().equals(currentUserId)) {
                                    currentUserVote = doc.getString("type");
                                    break;
                                }
                            }
                            updateVoteUI(currentUserVote);
                        } else {
                            updateVoteUI(null);
                        }
                    }
                });
    }

    private void updateVote(String voteType) {
        if (currentUserId == null) {
            Toast.makeText(this, "Login required to vote", Toast.LENGTH_SHORT).show();
            return;
        }

        if (voteType.equals(currentUserVote)) {

            db.collection("posts").document(postId)
                    .collection("votes").document(currentUserId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        currentUserVote = null;
                        updateVoteUI(null);

                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to remove vote", Toast.LENGTH_SHORT).show()
                    );
        } else {

            Map<String, Object> voteData = new HashMap<>();
            voteData.put("type", voteType);
            voteData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("posts").document(postId)
                    .collection("votes").document(currentUserId)
                    .set(voteData)
                    .addOnSuccessListener(aVoid -> {
                        currentUserVote = voteType;
                        updateVoteUI(voteType);

                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to vote", Toast.LENGTH_SHORT).show()
                    );
        }
    }


    private void updateVoteUI(String userVote) {
        if ("upvote".equals(userVote)) {
            btnUpvote.setBackgroundColor(getResources().getColor(R.color.vote_selected));
            btnDownvote.setBackgroundColor(getResources().getColor(R.color.vote_unselected));
        } else if ("downvote".equals(userVote)) {
            btnUpvote.setBackgroundColor(getResources().getColor(R.color.vote_unselected));
            btnDownvote.setBackgroundColor(getResources().getColor(R.color.vote_selected));
        } else {
            btnUpvote.setBackgroundColor(getResources().getColor(R.color.vote_unselected));
            btnDownvote.setBackgroundColor(getResources().getColor(R.color.vote_unselected));
        }
    }

    private void setupMiniMap() {
        try {
            miniMapView.setMultiTouchControls(false);
            miniMapView.setClickable(false);
            miniMapView.getController().setZoom(15.0);

            if (latitude != 0.0 && longitude != 0.0) {
                GeoPoint postLocation = new GeoPoint(latitude, longitude);
                miniMapView.getController().setCenter(postLocation);

                Marker marker = new Marker(miniMapView);
                marker.setPosition(postLocation);
                marker.setTitle("Post Location");
                miniMapView.getOverlays().add(marker);

                miniMapView.setOnClickListener(v -> openFullMap());

                Button btnOpenFullMap = findViewById(R.id.btnOpenFullMap);
                btnOpenFullMap.setOnClickListener(v -> openFullMap());
            }
        } catch (Exception e) {
            Log.e("PostDetailActivity", "Error setting up mini map", e);
        }
    }

    private void openFullMap() {
        Intent intent = new Intent(PostDetailActivity.this, MapActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        startActivity(intent);
    }

    private void setupComments() {
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnPostComment = findViewById(R.id.btnPostComment);

        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);

        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((QuerySnapshot snapshot, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.e("PostDetailActivity", "Error listening to comments", e);
                        return;
                    }

                    commentList.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            try {
                                Comment comment = doc.toObject(Comment.class);
                                commentList.add(comment);
                            } catch (Exception ex) {
                                Log.e("PostDetailActivity", "Error parsing comment", ex);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                    }
                });

        btnPostComment.setOnClickListener(v -> postComment());
    }

    private void postComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPostComment.setEnabled(false);

        getCurrentUsername(currentUsername -> {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("user", currentUsername);
            commentData.put("text", text);
            commentData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .add(commentData)
                    .addOnSuccessListener(documentReference -> {
                        etComment.setText("");
                        btnPostComment.setEnabled(true);
                        Log.d("PostDetailActivity", "Comment posted successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PostDetailActivity", "Error posting comment", e);
                        Toast.makeText(PostDetailActivity.this, "Failed to post comment", Toast.LENGTH_SHORT).show();
                        btnPostComment.setEnabled(true);
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniMapView != null) {
            miniMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniMapView != null) {
            miniMapView.onPause();
        }
    }
}

class Comment {
    private String user;
    private String text;
    private com.google.firebase.Timestamp timestamp;

    public Comment() {}

    public Comment(String user, String text) {
        this.user = user;
        this.text = text;
        this.timestamp = com.google.firebase.Timestamp.now();
    }

    public String getUser() { return user; }
    public String getText() { return text; }
    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }

    public void setUser(String user) { this.user = user; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }
}

class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.user.setText(comment.getUser() != null ? comment.getUser() : "Anonymous");
        holder.text.setText(comment.getText() != null ? comment.getText() : "");

        if (comment.getTimestamp() != null) {
            long timestampMillis = comment.getTimestamp().toDate().getTime();
            String timeAgo = formatTimeAgo(timestampMillis);
            holder.timestamp.setText(timeAgo);
        } else {
            holder.timestamp.setText("");
        }
    }

    private String formatTimeAgo(long timestampMillis) {
        long now = System.currentTimeMillis();
        long diff = now - timestampMillis;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) {
            return "1 minute ago";
        } else if (hours < 1) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (days <= 5) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        } else {

            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            return dateFormat.format(new java.util.Date(timestampMillis));
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView user, text, timestamp;

        CommentViewHolder(View itemView) {
            super(itemView);
            user = itemView.findViewById(R.id.tvCommentUser);
            text = itemView.findViewById(R.id.tvCommentText);
            timestamp = itemView.findViewById(R.id.tvCommentTimestamp);
        }
    }
}