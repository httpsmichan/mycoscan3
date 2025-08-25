package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JournalActivity extends AppCompatActivity {

    private EditText etNote;
    private ListView listViewNotes;
    private ArrayList<String> noteList;
    private ArrayList<String> noteIds; // to track Firestore doc IDs
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db;
    private String userId;
    private CollectionReference notesRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal);

        etNote = findViewById(R.id.etNote);
        listViewNotes = findViewById(R.id.listViewNotes);

        noteList = new ArrayList<>();
        noteIds = new ArrayList<>();

        // Custom adapter with 3-line limit
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, noteList) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);

                textView.setMaxLines(3);
                textView.setEllipsize(android.text.TextUtils.TruncateAt.END);

                return view;
            }
        };

        listViewNotes.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        notesRef = db.collection("users").document(userId).collection("notes");

        loadNotes();

        // Write a new note (via modal)
        etNote.setOnClickListener(v -> showNoteDialog(null, null));

        // Edit/Delete when a note is clicked
        listViewNotes.setOnItemClickListener((parent, view, position, id) -> {
            String noteId = noteIds.get(position);
            String noteText = noteList.get(position);
            showNoteDialog(noteId, noteText);
        });
    }

    private void loadNotes() {
        notesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            noteList.clear();
            noteIds.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String note = doc.getString("text");
                noteList.add(note);
                noteIds.add(doc.getId());
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showNoteDialog(String noteId, String oldText) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottomsheet_note);

        EditText input = dialog.findViewById(R.id.etDialogNote);
        TextView title = dialog.findViewById(R.id.tvDialogTitle);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnDelete = dialog.findViewById(R.id.btnDelete);

        if (noteId != null) {
            if (input != null) input.setText(oldText);
            if (title != null) title.setText("Edit Note");
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String noteText = input.getText().toString().trim();
                if (noteText.isEmpty()) {
                    Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (noteId == null) {
                    Map<String, Object> note = new HashMap<>();
                    note.put("text", noteText);
                    notesRef.add(note).addOnSuccessListener(ref -> {
                        loadNotes();
                        Toast.makeText(this, "Note added!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                } else {
                    DocumentReference docRef = notesRef.document(noteId);
                    docRef.update("text", noteText).addOnSuccessListener(aVoid -> {
                        loadNotes();
                        Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (noteId != null) {
                    notesRef.document(noteId).delete().addOnSuccessListener(aVoid -> {
                        loadNotes();
                        Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }
            });
        }

        dialog.show();
    }
}
