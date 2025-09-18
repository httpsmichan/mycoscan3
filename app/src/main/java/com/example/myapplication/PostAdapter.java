package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Username
        holder.tvDetailUser.setText(post.getUsername() != null ? post.getUsername() : "Unknown");

        // Timestamp formatting
        if (post.getTimestamp() > 0) {
            long now = System.currentTimeMillis();
            long diff = now - post.getTimestamp();

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            String timeText;
            if (hours < 24) {
                if (hours == 0) {
                    timeText = (minutes <= 1) ? "1 minute ago" : minutes + " minutes ago";
                } else {
                    timeText = hours + (hours == 1 ? " hour ago" : " hours ago");
                }
            } else if (days <= 5) {
                timeText = days + (days == 1 ? " day ago" : " days ago");
            } else {
                timeText = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(new Date(post.getTimestamp()));
            }

            holder.tvTimeStamp.setText(timeText);
        } else {
            holder.tvTimeStamp.setText("Unknown time");
        }

        // Verified status with shield badges
        String verified = post.getVerified() != null ? post.getVerified() : "Not Verified";

        if (verified.equalsIgnoreCase("verified")) {
            holder.ivVerificationBadge.setImageResource(R.drawable.ic_trusted_badge);
            holder.ivVerificationBadge.setVisibility(View.VISIBLE);
            holder.tvVerifiedStatus.setVisibility(View.GONE);
        } else if (verified.equalsIgnoreCase("Unreliable")) {
            holder.ivVerificationBadge.setImageResource(R.drawable.ic_not_trusted_badge);
            holder.ivVerificationBadge.setVisibility(View.VISIBLE);
            holder.tvVerifiedStatus.setVisibility(View.GONE);
        } else {
            // For "Not Verified" or pending posts, hide the badge
            holder.ivVerificationBadge.setVisibility(View.GONE);
            holder.tvVerifiedStatus.setText("Pending Review");
            holder.tvVerifiedStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.darker_gray));
            holder.tvVerifiedStatus.setVisibility(View.VISIBLE);
        }

        // Mushroom type & description
        holder.tvMushroomType.setText(post.getMushroomType() != null ? post.getMushroomType() : "Unknown type");
        holder.tvDescription.setText(post.getDescription() != null ? post.getDescription() : "");

        // --------------------------
        // Load image with debug logging (replaced section)
        // --------------------------
        String imageUrl = post.getImageUrl();
        Log.d("PostAdapter", "Loading image for post " + post.getPostId() + " with URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("PostAdapter", "Image URL is valid, loading with Glide");

            Glide.with(holder.ivPostImage.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_close_clear_cancel) // Add error placeholder
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                                    Object model,
                                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                    boolean isFirstResource) {
                            Log.e("PostAdapter", "Glide failed to load image: " + imageUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                       Object model,
                                                       com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                       com.bumptech.glide.load.DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d("PostAdapter", "Glide successfully loaded image: " + imageUrl);
                            return false;
                        }
                    })
                    .into(holder.ivPostImage);
        } else {
            Log.d("PostAdapter", "Image URL is null or empty, using placeholder");
            holder.ivPostImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Open PostDetailActivity on click
        holder.itemView.setOnClickListener(v -> {
            Log.d("PostAdapter", "Clicking post with ID: " + post.getPostId());

            Intent intent = new Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            intent.putExtra("imageUrl", post.getImageUrl());
            intent.putExtra("mushroomType", post.getMushroomType());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("userId", post.getUserId());
            intent.putExtra("username", post.getUsername());
            intent.putExtra("latitude", post.getLatitude());
            intent.putExtra("longitude", post.getLongitude());
            intent.putExtra("verified", post.getVerified());

            v.getContext().startActivity(intent);
        });

        // Three-dot menu (Delete / Report)
        holder.menuOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.menuOptions);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null && post.getUserId().equals(currentUser.getUid())) {
                popup.getMenu().add("Delete");
            } else {
                popup.getMenu().add("Report");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Delete")) {
                    deletePost(post.getPostId(), position);
                } else if (item.getTitle().equals("Report")) {
                    reportPost(post.getPostId());
                }
                return true;
            });

            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private void deletePost(String postId, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    postList.remove(position);
                    notifyItemRemoved(position);
                    Log.d("PostAdapter", "Post deleted: " + postId);
                })
                .addOnFailureListener(e -> Log.e("PostAdapter", "Error deleting post", e));
    }

    private void reportPost(String postId) {
        Intent intent = new Intent(context, ReportActivity.class);
        intent.putExtra("postId", postId);
        context.startActivity(intent);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetailUser, tvTimeStamp, tvMushroomType, tvDescription, tvVerifiedStatus;
        ImageView ivPostImage, menuOptions, ivVerificationBadge; // Added badge ImageView

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDetailUser = itemView.findViewById(R.id.tvDetailUser);
            tvTimeStamp = itemView.findViewById(R.id.tvTimeStamp);
            tvVerifiedStatus = itemView.findViewById(R.id.tvVerifiedStatus);
            tvMushroomType = itemView.findViewById(R.id.tvMushroomType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            menuOptions = itemView.findViewById(R.id.menuOptions);
            ivVerificationBadge = itemView.findViewById(R.id.ivVerificationBadge); // Add this line
        }
    }
}
