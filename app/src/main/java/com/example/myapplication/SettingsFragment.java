package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsFragment extends Fragment {

    private TextView textUserEmail;
    private Button btnEditProfile, btnSecurity, btnJournal, btnLogout;
    private RecyclerView recyclerUserPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_settings_fragment, container, false);

        textUserEmail = view.findViewById(R.id.textUserEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSecurity = view.findViewById(R.id.btnSecurity);
        btnJournal = view.findViewById(R.id.btnJournal);
        btnLogout = view.findViewById(R.id.btnLogout);
        recyclerUserPosts = view.findViewById(R.id.recyclerUserPosts);

        recyclerUserPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerUserPosts.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();

        // Show current signed-in email and load posts
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            textUserEmail.setText("Hi! " + user.getEmail());
            loadUserPosts(user.getUid());
        } else {
            textUserEmail.setText("No user signed in");
        }

        // Navigation
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), EditProfileActivity.class)));
        btnSecurity.setOnClickListener(v -> startActivity(new Intent(getActivity(), SecurityActivity.class)));
        btnJournal.setOnClickListener(v -> startActivity(new Intent(getActivity(), JournalActivity.class)));

        // Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
    }

    private void loadUserPosts(String userId) {
        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        postList.add(post);
                    }

                    // Sort locally by timestamp descending
                    Collections.sort(postList, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                    postAdapter.notifyDataSetChanged();
                });
    }
}
