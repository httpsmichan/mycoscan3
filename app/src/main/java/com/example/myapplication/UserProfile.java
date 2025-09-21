package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class UserProfile extends AppCompatActivity {

    private Button btnFollow;
    private FirebaseFirestore db;
    private String visitedUserId;  // the user whose profile is being viewed
    private String currentUserId;  // the logged-in user
    private boolean isFollowing = false; // track state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firestore + Auth
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // TODO: Pass visitedUserId through Intent when opening UserProfile
        visitedUserId = getIntent().getStringExtra("visitedUserId");

        btnFollow = findViewById(R.id.btnFollow);

        btnFollow.setOnClickListener(v -> toggleFollow());
    }

    private void toggleFollow() {
        if (visitedUserId == null || currentUserId == null) {
            Toast.makeText(this, "Error: Missing user info", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference visitedUserRef = db.collection("users").document(visitedUserId);
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);

        if (!isFollowing) {
            // FOLLOW → increment visitedUser's followers and currentUser's following
            visitedUserRef.update("followers", FieldValue.increment(1));
            currentUserRef.update("following", FieldValue.increment(1));

            btnFollow.setText("Unfollow");
            isFollowing = true;
        } else {
            // UNFOLLOW → decrement
            visitedUserRef.update("followers", FieldValue.increment(-1));
            currentUserRef.update("following", FieldValue.increment(-1));

            btnFollow.setText("Follow");
            isFollowing = false;
        }
    }
}
