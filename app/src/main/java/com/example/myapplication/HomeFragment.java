package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class HomeFragment extends Fragment {

    private FloatingActionButton fabReport;
    private RecyclerView recyclerPosts;
    private PostAdapter adapter;
    private final List<Post> postList = new ArrayList<>();
    private FirebaseFirestore db;

    private TextView tipTextView;

    public HomeFragment() {}

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerPosts.setHasFixedSize(true);

        adapter = new PostAdapter(requireContext(), postList);
        recyclerPosts.setAdapter(adapter);

        tipTextView = view.findViewById(R.id.tipTextView);

        db = FirebaseFirestore.getInstance();
        checkUserTerms();
        loadAllPosts();

        fabReport = view.findViewById(R.id.fabReport);

        fabReport.setOnClickListener(v -> {
            // Show a dialog form
            showReportDialog();
        });

        List<String> tips = loadTipsFromJson();
        if (!tips.isEmpty()) {
            String dailyTip = getDailyTip(tips);
            tipTextView.setText(dailyTip);
        }

        return view;
    }

    private void checkUserTerms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("HomeFragment", "No logged in user");
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean accepted = document.getBoolean("acceptedTerms");
                        if (accepted != null && accepted) {

                            loadAllPosts();
                            List<String> tips = loadTipsFromJson();
                            if (!tips.isEmpty()) {
                                String dailyTip = getDailyTip(tips);
                                tipTextView.setText(dailyTip);
                            }
                        } else {
                            Intent intent = new Intent(requireContext(), TermsActivity.class);
                            startActivity(intent);
                            requireActivity().finish();
                        }
                    } else {
                        Log.e("HomeFragment", "User document not found");
                    }
                })
                .addOnFailureListener(e -> Log.e("HomeFragment", "Error checking terms", e));
    }

    private void loadAllPosts() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    postList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Post post = doc.toObject(Post.class);
                        post.setPostId(doc.getId());
                        postList.add(post);

                        Log.d("HomeFragment", "Loaded post with ID: " + doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("HomeFragment", "Loaded " + postList.size() + " posts");
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading posts", e);
                });
    }

    private List<String> loadTipsFromJson() {
        List<String> tips = new ArrayList<>();
        try {
            InputStream is = requireContext().getAssets().open("tips.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray("tips");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject tipObject = jsonArray.getJSONObject(i);
                tips.add(tipObject.getString("tip"));
            }
        } catch (IOException | org.json.JSONException e) {
            Log.e("HomeFragment", "Error loading tips", e);
        }
        return tips;
    }

    private String getDailyTip(List<String> tips) {
        SharedPreferences prefs = requireContext().getSharedPreferences("DailyTipPrefs", requireContext().MODE_PRIVATE);

        int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int lastDay = prefs.getInt("last_day", -1);

        if (today != lastDay) {
            Random random = new Random();
            String newTip = tips.get(random.nextInt(tips.size()));

            prefs.edit()
                    .putInt("last_day", today)
                    .putString("tip_of_day", newTip)
                    .apply();

            return newTip;
        } else {
            return prefs.getString("tip_of_day", "Stay curious about mushrooms!");
        }
    }

    private void showReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Experiencing a problem?");
        builder.setMessage("Share it with us!");

        // Input field
        final EditText input = new EditText(requireContext());
        input.setHint("Describe the issue...");
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String reportText = input.getText().toString().trim();
            if (!reportText.isEmpty()) {
                // Save to Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                String userId = (user != null) ? user.getUid() : "anonymous";

                // Create report object
                HashMap<String, Object> report = new HashMap<>();
                report.put("label", "app report");
                report.put("message", reportText);
                report.put("userId", userId);
                report.put("timestamp", System.currentTimeMillis());

                db.collection("reports")
                        .add(report)
                        .addOnSuccessListener(docRef ->
                                Toast.makeText(requireContext(), "Report submitted. Thank you!", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );

            } else {
                Toast.makeText(requireContext(), "Please enter something", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

}
