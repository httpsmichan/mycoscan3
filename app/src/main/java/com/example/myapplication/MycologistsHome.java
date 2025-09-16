package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MycologistsHome extends Fragment {

    private RecyclerView recyclerView;
    private PostsAdapter adapter;
    private List<Post> postList;
    private FirebaseFirestore db;

    public MycologistsHome() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.activity_mycologists_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize post list and adapter
        postList = new ArrayList<>();
        adapter = new PostsAdapter(postList);
        recyclerView.setAdapter(adapter);

        // Fetch posts from Firebase
        fetchPostsFromFirebase();

        return view;
    }

    private void fetchPostsFromFirebase() {
        db.collection("posts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        postList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Post post = document.toObject(Post.class);
                                post.setPostId(document.getId()); // Use setPostId instead of setId
                                postList.add(post);
                            } catch (Exception e) {
                                Log.e("Firebase", "Error parsing document: " + e.getMessage());
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("Firebase", "Error getting documents: ", task.getException());
                        Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}