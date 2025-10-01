package com.example.myapplication;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private List<Post> postList;
    private FirebaseFirestore db;
    private boolean isHistory; // true = history, false = verification queue

    // Constructor
    public PostsAdapter(List<Post> postList, boolean isHistory) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
        this.isHistory = isHistory;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isHistory ? R.layout.item_post_history : R.layout.post_item;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new PostViewHolder(view, isHistory);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        Log.d("PostsAdapter", "Image URL: " + post.getImageUrl());

        // Null-safe Glide
        if (holder.imageView != null) {
            String imageUrl = post.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_favorite)
                        .error(R.drawable.ic_settings)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        if (holder.textMushroomType != null)
            holder.textMushroomType.setText(post.getMushroomType());

        if (holder.textPostedBy != null)
            holder.textPostedBy.setText("Posted by: " + post.getUsername());

        if (holder.textDescription != null)
            holder.textDescription.setText(post.getDescription());

        if (isHistory && holder.textVerified != null) {
            String status = post.getVerified();
            if (status != null) {
                holder.textVerified.setText(status.equalsIgnoreCase("unreliable") ? "Unreliable" : status);
                holder.textVerified.setBackgroundColor(status.equalsIgnoreCase("unreliable") ? Color.RED : Color.parseColor("#4CAF50")); // green for verified
            } else {
                holder.textVerified.setVisibility(View.GONE); // hide if null
            }
        }

        // Queue tab buttons
        if (!isHistory) {
            if (holder.btnVerify != null) holder.btnVerify.setVisibility(View.VISIBLE);
            if (holder.btnDecline != null) holder.btnDecline.setVisibility(View.VISIBLE);

            if ("verified".equals(post.getVerified()) || "unreliable".equals(post.getVerified())) {
                if (holder.btnVerify != null) holder.btnVerify.setVisibility(View.GONE);
                if (holder.btnDecline != null) holder.btnDecline.setVisibility(View.GONE);
            }

            if (holder.btnVerify != null)
                holder.btnVerify.setOnClickListener(v -> updatePostVerification(post.getPostId(), "verified", position));

            if (holder.btnDecline != null)
                holder.btnDecline.setOnClickListener(v -> updatePostVerification(post.getPostId(), "unreliable", position));
        } else {
            // History tab: hide buttons if layout has them
            if (holder.btnVerify != null) holder.btnVerify.setVisibility(View.GONE);
            if (holder.btnDecline != null) holder.btnDecline.setVisibility(View.GONE);
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
                    postList.get(position).setVerified(status);
                    notifyItemChanged(position);
                });
    }

    public void updateList(List<Post> newList) {
        this.postList.clear();
        this.postList.addAll(newList);
        notifyDataSetChanged();
    }


    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textMushroomType, textPostedBy, textDescription, textVerified;
        Button btnVerify, btnDecline;

        public PostViewHolder(@NonNull View itemView, boolean isHistory) {
            super(itemView);
            // Make sure IDs match both layouts
            imageView = itemView.findViewById(R.id.imageViews);
            textMushroomType = itemView.findViewById(R.id.textMushroomType);
            textPostedBy = itemView.findViewById(R.id.textPostedBy);
            textDescription = itemView.findViewById(R.id.textDescription);

            if (isHistory) {
                textVerified = itemView.findViewById(R.id.textVerified); // only in history layout
            } else {
                btnVerify = itemView.findViewById(R.id.btnVerify);
                btnDecline = itemView.findViewById(R.id.btnDecline);
            }
        }
    }
}
