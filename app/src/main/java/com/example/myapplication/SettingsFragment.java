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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    private TextView textUserEmail;
    private Button btnEditProfile, btnSecurity, btnJournal, btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_settings_fragment, container, false);

        textUserEmail = view.findViewById(R.id.textUserEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSecurity = view.findViewById(R.id.btnSecurity);
        btnJournal = view.findViewById(R.id.btnJournal);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Show current signed-in email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            textUserEmail.setText("Hi! " + user.getEmail());
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
            // After logout, go back to login screen
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish(); // close current activity
        });

        return view;
    }
}
