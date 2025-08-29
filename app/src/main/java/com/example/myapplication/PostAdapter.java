package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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
                    if (minutes <= 1) {
                        timeText = "1 minute ago";
                    } else {
                        timeText = minutes + " minutes ago";
                    }
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

        // Verified status
        String verified = post.getVerified() != null ? post.getVerified() : "Not Verified";
        holder.tvVerifiedStatus.setText(verified);

        if (verified.equalsIgnoreCase("verified")) {
            holder.tvVerifiedStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.tvVerifiedStatus.setTextColor(holder.itemView.getResources().getColor(android.R.color.holo_red_dark));
        }

        // Mushroom type & description
        holder.tvMushroomType.setText(post.getMushroomType() != null ? post.getMushroomType() : "Unknown type");
        holder.tvDescription.setText(post.getDescription() != null ? post.getDescription() : "");

        // Load image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(holder.ivPostImage.getContext())
                    .load(post.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // Click → PostDetailActivity
        holder.itemView.setOnClickListener(v -> {
            // ✅ ADD LOGGING TO DEBUG
            Log.d("PostAdapter", "Clicking post with ID: " + post.getPostId());

            Intent intent = new Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("postId", post.getPostId()); // ← ADD THIS LINE!
            intent.putExtra("imageUrl", post.getImageUrl());
            intent.putExtra("mushroomType", post.getMushroomType());
            intent.putExtra("description", post.getDescription());
            intent.putExtra("username", post.getUsername());
            intent.putExtra("latitude", post.getLatitude());
            intent.putExtra("longitude", post.getLongitude());
            intent.putExtra("verified", post.getVerified());

            // ✅ ADD LOGGING TO VERIFY DATA
            Log.d("PostAdapter", "Passing postId: " + post.getPostId());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvDetailUser, tvTimeStamp, tvVerifiedStatus, tvMushroomType, tvDescription;
        ImageView ivPostImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDetailUser = itemView.findViewById(R.id.tvDetailUser);
            tvTimeStamp = itemView.findViewById(R.id.tvTimeStamp);
            tvVerifiedStatus = itemView.findViewById(R.id.tvVerifiedStatus);
            tvMushroomType = itemView.findViewById(R.id.tvMushroomType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
        }
    }
}