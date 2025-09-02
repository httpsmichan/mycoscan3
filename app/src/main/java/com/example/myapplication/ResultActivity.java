package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        ImageView resultImage = findViewById(R.id.resultImage);
        TextView resultText = findViewById(R.id.resultText);

        String photoUriString = getIntent().getStringExtra("photoUri");
        String prediction = getIntent().getStringExtra("prediction");

        if (photoUriString != null) {
            Uri photoUri = Uri.parse(photoUriString);
            resultImage.setImageURI(photoUri);
        }

        resultText.setText("Identified Mushroom: " + prediction);
    }
}
