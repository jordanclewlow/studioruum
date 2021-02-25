package com.example.studioruum;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class NoteActivity extends AppCompatActivity {
    LocalDB localDB = new LocalDB(this);
    Spinner noteDrpDwn;
    TextView noteTitle;
    TextView noteContent;
    Button editNote;
    Button deleteNote;
    Button saveNewNote;
    Button saveNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        // Find activity elements
        noteDrpDwn = findViewById(R.id.noteDrpDwn);
        noteTitle = findViewById(R.id.noteTitle);
        noteContent = findViewById(R.id.noteContent);
        editNote = findViewById(R.id.editNoteBtn);
        deleteNote = findViewById(R.id.delNoteButton);
        saveNewNote = findViewById(R.id.newNoteBtn);
        saveNote = findViewById(R.id.updateNoteBtn);

        // Populate drop down with notes from local DB
        populateDropDown();
    }

    // Populates notes drop down menu with data from local DB
    public void populateDropDown() {
        final List<Note> notes = localDB.allNotes();

        if (notes.size() == 0) {
            Note error = new Note(0, 0, "No notes in DB", "");
            List<Note> noNotes = new ArrayList<Note>();
            noNotes.add(error);
            ArrayAdapter<Note> noteAdapter = new ArrayAdapter<Note>(this, android.R.layout.simple_spinner_item, noNotes);
            noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            noteDrpDwn.setAdapter(noteAdapter);
            deleteNote.setEnabled(false);
            saveNote.setEnabled(false);
            noteTitle.setText("");
            noteContent.setText("");
            noteTitle.setHint("Note title goes here...");
            noteContent.setHint("Note content goes here...");
            Toast toast = Toast.makeText(NoteActivity.this, "No notes in DB", Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            ArrayAdapter<Note> noteAdapter = new ArrayAdapter<Note>(this, android.R.layout.simple_spinner_item, notes);
            noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            noteDrpDwn.setAdapter(noteAdapter);
            noteDrpDwn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Note selected = (Note) parent.getSelectedItem();

                    if (selected.getTitle() != "No notes in DB") {
                        noteTitle.setText(selected.getTitle());
                        noteContent.setText(selected.getContent());

                        deleteNote.setEnabled(true);
                        saveNote.setEnabled(true);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    // Saves new note in local DB
    public void saveNewNote(View view){
        // Get title and content of note
        String title = String.valueOf(noteTitle.getText());
        String content = String.valueOf(noteContent.getText());

        // Save note and refresh drop-down menu content, select new item
        localDB.saveNote(title, content);
        populateDropDown();
        noteDrpDwn.setSelection(noteDrpDwn.getCount() - 1);

        // Disable text-views again
        noteTitle.setEnabled(false);
        noteContent.setEnabled(false);
    }

    // Updates a note in local DB
    public void updateNote(View view) {
        // Get id, title and content of note
        Note selected = (Note) noteDrpDwn.getSelectedItem();
        int id = selected.getDict();
        String title = String.valueOf(noteTitle.getText());
        String content = String.valueOf(noteContent.getText());

        // Get index of selected note
        int index = noteDrpDwn.getSelectedItemPosition();

        // Update record and refresh drop-down menu content, select modified item
        localDB.updateNote(id, title, content);
        populateDropDown();
        noteDrpDwn.setSelection(index);

        // Disable text-views again
        noteTitle.setEnabled(false);
        noteContent.setEnabled(false);
    }

    // Deletes selected note from local DB
    public void deleteNote(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(NoteActivity.this).create();
        alertDialog.setTitle("Deletion Alert");
        alertDialog.setMessage("Are you sure you want to delete this note?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Get id and index of selected note
                        Note selected = (Note) noteDrpDwn.getSelectedItem();
                        int id = selected.getDict();
                        int index = noteDrpDwn.getSelectedItemPosition();

                        // Delete note refresh drop-down menu content
                        localDB.deleteNote(id);
                        populateDropDown();

                        // Select preceding note if there are notes in DB or not first note
                        if (localDB.allNotes().size() > 0 || index > 0) {
                            noteDrpDwn.setSelection(index - 1);
                        }
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    // Enables text views for title and content
    public void makeEnabled(View view) {
        noteTitle.setEnabled(true);
        noteContent.setEnabled(true);
    }
}
