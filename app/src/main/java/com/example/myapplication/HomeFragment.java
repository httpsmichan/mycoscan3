package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerPosts;
    private PostAdapter adapter;
    private final List<Post> postList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration registration;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPosts.setHasFixedSize(true);

        adapter = new PostAdapter(postList);
        recyclerPosts.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        listenForPosts(); // live updates

        return view;
    }

    private void listenForPosts() {
        // Order by timestamp DESC (you already write "timestamp" in UploadFragment)
        registration = db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null || snapshots == null) return;

                        postList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Post post = doc.toObject(Post.class);
                            // (Optional) If you later add "username" to each post, extend the model and bind it too.
                            postList.add(post);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    /** Adapter kept inside the fragment (no extra file) */
    private static class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostVH> {
        private final List<Post> items;

        PostAdapter(List<Post> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post, parent, false);
            return new PostVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PostVH h, int position) {
            Post p = items.get(position);

            h.tvMushroomType.setText(p.getMushroomType() == null ? "Unknown type" : p.getMushroomType());
            h.tvDescription.setText(p.getDescription() == null ? "" : p.getDescription());

            // Load image if available
            if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                Glide.with(h.ivPostImage.getContext())
                        .load(p.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_report_image)
                        .into(h.ivPostImage);
            } else {
                h.ivPostImage.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PostVH extends RecyclerView.ViewHolder {
            ImageView ivPostImage;
            TextView tvMushroomType, tvDescription;

            PostVH(@NonNull View itemView) {
                super(itemView);
                ivPostImage = itemView.findViewById(R.id.ivPostImage);
                tvMushroomType = itemView.findViewById(R.id.tvMushroomType);
                tvDescription = itemView.findViewById(R.id.tvDescription);
            }
        }
    }
}
