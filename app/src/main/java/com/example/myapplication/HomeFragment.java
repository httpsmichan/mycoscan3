package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerPosts;
    private PostAdapter adapter;
    private final List<Post> postList = new ArrayList<>();

    private FirebaseFirestore db;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPosts.setHasFixedSize(true);

        // ✅ use your existing PostAdapter (not inner class)
        adapter = new PostAdapter(requireContext(), postList);
        recyclerPosts.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadAllPosts(); // one-time fetch

        return view;
    }

    private void loadAllPosts() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId()); // ✅ SET THE POST ID HERE!
                        postList.add(post);

                        // Debug logging
                        Log.d("HomeFragment", "Loaded post with ID: " + doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("HomeFragment", "Loaded " + postList.size() + " posts");
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading posts", e);
                });
    }
}