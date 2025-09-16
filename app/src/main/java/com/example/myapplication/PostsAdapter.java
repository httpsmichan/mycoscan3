package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private List<Post> postList;
    private FirebaseFirestore db;

    public PostsAdapter(List<Post> postList) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.post_item, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Load image using Glide
        Glide.with(holder.itemView.getContext())
                .load(post.getImageUrl())
                .placeholder(R.drawable.ic_favorite) // Add a placeholder image
                .error(R.drawable.ic_settings) // Add an error image
                .into(holder.imageView);

        holder.textMushroomType.setText(post.getMushroomType());
        holder.textPostedBy.setText("Posted by: " + post.getUsername());
        holder.textDescription.setText(post.getDescription());

        // Set up verify button click
        holder.btnVerify.setOnClickListener(v -> {
            updatePostVerification(post.getPostId(), "verified", position);
            Toast.makeText(holder.itemView.getContext(), "Post verified!", Toast.LENGTH_SHORT).show();
        });

// Set up decline button click
        holder.btnDecline.setOnClickListener(v -> {
            updatePostVerification(post.getPostId(), "Unreliable", position);
            Toast.makeText(holder.itemView.getContext(), "Post marked as unreliable!", Toast.LENGTH_SHORT).show();
        });

        // Hide buttons if already verified/declined
        if ("verified".equals(post.getVerified()) || "Unreliable".equals(post.getVerified())) {
            holder.btnVerify.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private void updatePostVerification(String postId, String status, int position) {
        db.collection("posts").document(postId)
                .update("verified", status)
                .addOnSuccessListener(aVoid -> {
                    // Update local data
                    postList.get(position).setVerified(status);
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textMushroomType, textPostedBy, textDescription;
        Button btnVerify, btnDecline;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViews);
            textMushroomType = itemView.findViewById(R.id.textMushroomType);
            textPostedBy = itemView.findViewById(R.id.textPostedBy);
            textDescription = itemView.findViewById(R.id.textDescription);
            btnVerify = itemView.findViewById(R.id.btnVerify);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}