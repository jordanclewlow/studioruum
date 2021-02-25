package com.example.studioruum;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button goFlashcards;
    Button goNotes;
    Button goDictionary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find goNotes button and add listener to redirect to notes page
        goNotes = findViewById(R.id.goNotesBtn);
        goNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                startActivity(intent);
            }
        });

        // Find goDictionary button and add listener to redirect to dictionaries page
        goDictionary = findViewById(R.id.goDictBtn);
        goDictionary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DictionaryActivity.class);
                startActivity(intent);
            }
        });

        // Find goFlashcards button and add listener to redirect to dictionaries page
        goFlashcards = findViewById(R.id.goFlashBtn);
        goFlashcards.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FlashcardActivity.class);
                startActivity(intent);
            }
        });
    }

}
