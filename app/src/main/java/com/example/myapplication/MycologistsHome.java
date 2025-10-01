package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MycologistsHome extends Fragment {

    private RecyclerView recyclerViewQueue, recyclerViewHistory;
    private PostsAdapter queueAdapter, historyAdapter;
    private List<Post> queueList, historyList;
    private FirebaseFirestore db;
    private TextView tvNoPosts;
    private EditText etSearch;
    private TextView spinnerText;

    // Store original unfiltered lists
    private List<Post> allQueuePosts = new ArrayList<>();
    private List<Post> allHistoryPosts = new ArrayList<>();

    // Current filter selection
    private String currentFilter = "All";

    public MycologistsHome() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_mycologists_home, container, false);

        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerViews
        recyclerViewQueue = view.findViewById(R.id.recyclerViewPosts);
        recyclerViewQueue.setLayoutManager(new LinearLayoutManager(getContext()));
        queueList = new ArrayList<>();
        queueAdapter = new PostsAdapter(queueList, false);
        recyclerViewQueue.setAdapter(queueAdapter);

        recyclerViewHistory = view.findViewById(R.id.recyclerViewHistory);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList = new ArrayList<>();
        historyAdapter = new PostsAdapter(historyList, true);
        recyclerViewHistory.setAdapter(historyAdapter);

        // No posts text
        tvNoPosts = view.findViewById(R.id.tvNoPosts);

        // Initialize search field
        etSearch = view.findViewById(R.id.etSearch);
        spinnerText = view.findViewById(R.id.spinnerText);
        spinnerText.setText("All");

        // Setup search functionality
        setupSearch();

        // Setup filter functionality
        setupFilter(view);

        // Tabs
        TextView tabQueue = view.findViewById(R.id.tabQueue);
        TextView tabHistory = view.findViewById(R.id.tabHistory);

        tabQueue.setOnClickListener(v -> {
            recyclerViewQueue.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.GONE);

            tabQueue.setBackgroundResource(R.drawable.selected);
            tabQueue.setTextColor(Color.WHITE);
            tabHistory.setBackgroundResource(R.drawable.not_selected);
            tabHistory.setTextColor(Color.parseColor("#047857"));

            // Show tvNoPosts in queue tab if empty
            if (queueList.isEmpty()) {
                tvNoPosts.setVisibility(View.VISIBLE);
            } else {
                tvNoPosts.setVisibility(View.GONE);
            }
        });

        tabHistory.setOnClickListener(v -> {
            recyclerViewQueue.setVisibility(View.GONE);
            recyclerViewHistory.setVisibility(View.VISIBLE);

            tabHistory.setBackgroundResource(R.drawable.selected);
            tabHistory.setTextColor(Color.WHITE);
            tabQueue.setBackgroundResource(R.drawable.not_selected);
            tabQueue.setTextColor(Color.parseColor("#047857"));

            // Always hide tvNoPosts in history tab
            tvNoPosts.setVisibility(View.GONE);
        });

        // Fetch posts from Firebase
        fetchPostsFromFirebase();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchAndFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilter(View view) {
        View filterRow = (View) view.findViewById(R.id.spinnerText).getParent();

        filterRow.setOnClickListener(v -> {
            String[] options = {"All", "Verified", "Unreliable"};

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            builder.setTitle("Filter Posts")
                    .setItems(options, (dialog, which) -> {
                        currentFilter = options[which];
                        spinnerText.setText(currentFilter);

                        // Apply both search and filter
                        applySearchAndFilter(etSearch.getText().toString());
                    });
            builder.show();
        });
    }

    private void applySearchAndFilter(String searchQuery) {
        List<Post> filteredQueue = new ArrayList<>();
        List<Post> filteredHistory = new ArrayList<>();

        // First, apply filter based on verification status
        List<Post> filterQueue = new ArrayList<>();
        List<Post> filterHistory = new ArrayList<>();

        if (currentFilter.equals("All")) {
            filterQueue.addAll(allQueuePosts);
            filterHistory.addAll(allHistoryPosts);
        } else if (currentFilter.equals("Verified")) {
            for (Post post : allHistoryPosts) {
                if ("verified".equalsIgnoreCase(post.getVerified())) {
                    filterHistory.add(post);
                }
            }
        } else if (currentFilter.equals("Unreliable")) {
            for (Post post : allHistoryPosts) {
                if ("unreliable".equalsIgnoreCase(post.getVerified())) {
                    filterHistory.add(post);
                }
            }
        }

        // Then apply search query
        String query = searchQuery.toLowerCase().trim();

        if (query.isEmpty()) {
            filteredQueue = filterQueue;
            filteredHistory = filterHistory;
        } else {
            // Search in queue
            for (Post post : filterQueue) {
                String username = post.getUsername() != null ? post.getUsername().toLowerCase() : "";
                String mushroomType = post.getMushroomType() != null ? post.getMushroomType().toLowerCase() : "";

                if (username.contains(query) || mushroomType.contains(query)) {
                    filteredQueue.add(post);
                }
            }

            // Search in history
            for (Post post : filterHistory) {
                String username = post.getUsername() != null ? post.getUsername().toLowerCase() : "";
                String mushroomType = post.getMushroomType() != null ? post.getMushroomType().toLowerCase() : "";

                if (username.contains(query) || mushroomType.contains(query)) {
                    filteredHistory.add(post);
                }
            }
        }

        // Update adapters
        queueAdapter.updateList(filteredQueue);
        historyAdapter.updateList(filteredHistory);

        // Update "no posts" visibility
        if (recyclerViewQueue.getVisibility() == View.VISIBLE) {
            if (filteredQueue.isEmpty()) {
                tvNoPosts.setVisibility(View.VISIBLE);
            } else {
                tvNoPosts.setVisibility(View.GONE);
            }
        }
    }

    private void fetchPostsFromFirebase() {
        db.collection("posts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allQueuePosts.clear();
                        allHistoryPosts.clear();
                        queueList.clear();
                        historyList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            post.setPostId(document.getId());

                            if (post.getVerified() == null) {
                                allQueuePosts.add(post);
                                queueList.add(post);
                            } else {
                                allHistoryPosts.add(post);
                                historyList.add(post);
                            }
                        }

                        queueAdapter.notifyDataSetChanged();
                        historyAdapter.notifyDataSetChanged();

                        // Show/hide tvNoPosts if queue tab is visible
                        if (recyclerViewQueue.getVisibility() == View.VISIBLE) {
                            if (queueList.isEmpty()) {
                                tvNoPosts.setVisibility(View.VISIBLE);
                            } else {
                                tvNoPosts.setVisibility(View.GONE);
                            }
                        }

                    } else {
                        Toast.makeText(getContext(), "Failed to load posts", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}