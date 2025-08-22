package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JournalActivity extends AppCompatActivity {

    private EditText etNote;
    private Button btnAddNote;
    private ListView listViewNotes;

    private ArrayList<String> noteList;
    private ArrayAdapter<String> adapter;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        etNote = findViewById(R.id.etNote);
        btnAddNote = findViewById(R.id.btnAddNote);
        listViewNotes = findViewById(R.id.listViewNotes);

        noteList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, noteList);
        listViewNotes.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        CollectionReference notesRef = db.collection("users").document(userId).collection("notes");

        // Load existing notes
        notesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            noteList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String note = doc.getString("text");
                noteList.add(note);
            }
            adapter.notifyDataSetChanged();
        });

        // Add new note
        btnAddNote.setOnClickListener(v -> {
            String noteText = etNote.getText().toString().trim();
            if (noteText.isEmpty()) {
                Toast.makeText(this, "Enter a note", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> note = new HashMap<>();
            note.put("text", noteText);

            notesRef.add(note)
                    .addOnSuccessListener(documentReference -> {
                        noteList.add(noteText);
                        adapter.notifyDataSetChanged();
                        etNote.setText("");
                        Toast.makeText(this, "Note added!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
