package com.example.studioruum;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class FlashcardActivity extends AppCompatActivity {
    LocalDB localDB = new LocalDB(this);
    Spinner dictDrpDwn;
    Spinner flashDrpDwn;
    Button newFlashBtn;
    Button editFlashBtn;
    Button deleteFlashBtn;
    Button flipFlashBtn;
    Button prevFlashBtn;
    Button nextFlashBtn;
    TextView flashContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        // Find and disable (some) view components
        dictDrpDwn = findViewById(R.id.flashDictDrpDwn);
        flashDrpDwn = findViewById(R.id.flashDrpDwn);
        flashDrpDwn.setEnabled(false);
        newFlashBtn = findViewById(R.id.newFlashBtn);
        newFlashBtn.setEnabled(false);
        editFlashBtn = findViewById(R.id.editFlashBtn);
        editFlashBtn.setEnabled(false);
        deleteFlashBtn = findViewById(R.id.deleteFlashBtn);
        deleteFlashBtn.setEnabled(false);
        flipFlashBtn = findViewById(R.id.flipFlashBtn);
        flipFlashBtn.setEnabled(false);
        prevFlashBtn = findViewById(R.id.prevFlashBtn);
        prevFlashBtn.setEnabled(false);
        nextFlashBtn = findViewById(R.id.nextFlashBtn);
        nextFlashBtn.setEnabled(false);
        flashContent = findViewById(R.id.flashContentView);

        // Populate dictionary drop down menu
        populateDictDropDown();
    }

    // Populates dictionaries drop down menu with data from local DB
    public void populateDictDropDown() {
        final List<Dictionary> dictionaries = localDB.allDictionaries();

        // If no dictionaries on DB, alert user
        if (dictionaries.size() == 0) {
            Dictionary error = new Dictionary(0, 0, "No dictionaries in DB");
            List<Dictionary> noDicts = new ArrayList<Dictionary>();
            noDicts.add(error);
            ArrayAdapter<Dictionary> noteAdapter = new ArrayAdapter<Dictionary>(this, android.R.layout.simple_spinner_item, noDicts);
            noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dictDrpDwn.setAdapter(noteAdapter);

            // Create and display alert
            AlertDialog alertDialog = new AlertDialog.Builder(FlashcardActivity.this).create();
            alertDialog.setTitle("No Dictionaries");
            alertDialog.setMessage("There are no existing dictionaries, do you want to create one?");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Go to dictionary page if user selects "yes
                            Intent intent = new Intent(FlashcardActivity.this, DictionaryActivity.class);
                            startActivity(intent);
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

        // If dictionaries in DB, populate dictionary drop down
        else {
            ArrayAdapter<Dictionary> dictAdapter = new ArrayAdapter<Dictionary>(this, android.R.layout.simple_spinner_item, dictionaries);
            dictAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dictDrpDwn.setAdapter(dictAdapter);
            newFlashBtn.setEnabled(true);

            // Set listener for item selection from dictionary drop-down
            dictDrpDwn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Get selected dictionary and id
                    Dictionary selectedDict = (Dictionary) parent.getSelectedItem();
                    int dictId = selectedDict.getDict();

                    // If dictionaries in DB, populate flashcards drop-down
                    if (selectedDict.getTitle() != "No dictionaries in DB" && selectedDict.resourceID != 0) {
                        populateFlashDropDown(dictId);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    public void populateFlashDropDown(int dictId) {
        List <Flashcard> fromSelected = localDB.allFlashcards(dictId);
        System.out.println("DEBUG - FROMSELECTED: " + fromSelected);

        // If dictionary contains flashcards, populate flashcards drop-down
        if (!fromSelected.isEmpty()) {
            ArrayAdapter<Flashcard> flashAdapter = new ArrayAdapter<Flashcard>(FlashcardActivity.this, android.R.layout.simple_spinner_item, fromSelected);
            flashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            flashDrpDwn.setAdapter(flashAdapter);
            flashDrpDwn.setEnabled(true);

            // Set listener for item selection in flashcard drop-down
            flashDrpDwn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Flashcard selectedFlashcard = (Flashcard) parent.getSelectedItem();
                    int index = parent.getSelectedItemPosition();

                    flashContent.setText(selectedFlashcard.frontProperty());
                    editFlashBtn.setEnabled(true);
                    deleteFlashBtn.setEnabled(true);
                    flipFlashBtn.setEnabled(true);

                    // Make "Prev" button enabled if not first flashcard
                    if (index != 0) {
                        prevFlashBtn.setEnabled(true);
                    }
                    else {
                        prevFlashBtn.setEnabled(false);
                    }

                    // Make "Next" button enabled if not last flashcard
                    if (index !=parent.getCount() - 1) {
                        nextFlashBtn.setEnabled(true);
                    }
                    else {
                        nextFlashBtn.setEnabled(false);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        else {
            flashDrpDwn.setEnabled(false);
            flashContent.setText("");
            flashContent.setEnabled(false);
            Toast toast = Toast.makeText(FlashcardActivity.this, "No flashcards in selected dictionary", Toast.LENGTH_SHORT);
            toast.show();

        }
    }

    // Creates a new flashcard in local DB
    public void createFlashcard(View view) {
        // Get selected dictionary and id
        Dictionary selectedDict = (Dictionary) dictDrpDwn.getSelectedItem();
        final int dictId = selectedDict.getDict();

        // If dictionaries in DB, create flashcard
        if (selectedDict.getTitle() != "No dictionaries in DB" && selectedDict.resourceID != 0) {
            // Create alert dialog
            AlertDialog alertDialog = new AlertDialog.Builder(FlashcardActivity.this).create();
            alertDialog.setTitle("Create new flashcard");

            // Create layout where to place input boxes
            LinearLayout layout = new LinearLayout(FlashcardActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);

            // Create front content input box and add to layout
            final EditText frontBox = new EditText(FlashcardActivity.this);
            frontBox.setHint("Front");
            layout.addView(frontBox);

            // Create back content input box and add to layout
            final EditText backBox = new EditText(FlashcardActivity.this);
            backBox.setHint("Back");
            layout.addView(backBox);

            // Set alert dialog view to be previously created layout
            alertDialog.setView(layout);

            // Listener for positive button, create flashcard
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String frontContent = String.valueOf(frontBox.getText());
                    String backContent = String.valueOf(backBox.getText());
                    localDB.saveFlashcard(dictId, frontContent, backContent);
                    populateFlashDropDown(dictId);
                    flashDrpDwn.setSelection(flashDrpDwn.getCount() - 1);
                }
            });

            // Listener for negative button, dismiss alert
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            alertDialog.show();
        }
    }

    // Alters flashcard in local DB
    public void editFlashcard(View view) {
        // Get selected flashcard info
        Flashcard selectedFlash = (Flashcard) flashDrpDwn.getSelectedItem();
        String frontContent = selectedFlash.frontProperty();
        String backContent = selectedFlash.backProperty();
        final int index = flashDrpDwn.getSelectedItemPosition();
        final int flashID = selectedFlash.getFID();
        final int dictId = selectedFlash.getDict();

        // Create alert dialog
        AlertDialog alertDialog = new AlertDialog.Builder(FlashcardActivity.this).create();
        alertDialog.setTitle("Edit flashcard");

        // Create layout where to place input boxes
        LinearLayout layout = new LinearLayout(FlashcardActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Create front content input box and add to layout
        final EditText frontBox = new EditText(FlashcardActivity.this);
        frontBox.setHint("Front");
        frontBox.setText(frontContent);
        layout.addView(frontBox);

        // Create back content input box and add to layout
        final EditText backBox = new EditText(FlashcardActivity.this);
        backBox.setHint("Back");
        backBox.setText(backContent);
        layout.addView(backBox);

        // Set alert dialog view to be previously created layout
        alertDialog.setView(layout);

        // Listener for positive button, create flashcard
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String frontEdited = String.valueOf(frontBox.getText());
                String backEdited = String.valueOf(backBox.getText());
                localDB.updateFlashcard(flashID, frontEdited, backEdited);
                populateFlashDropDown(dictId);
                flashDrpDwn.setSelection(index);
            }
        });

        // Listener for negative button, dismiss alert
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    // Deletes flashcard from local DB
    public void deleteFlashcard(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(FlashcardActivity.this).create();
        alertDialog.setTitle("Deletion Alert");
        alertDialog.setMessage("Are you sure you want to delete this flashcard?");

        // Listener for positive button, delete dictionary
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Get id and index of selected flashcard
                        Flashcard selectedFlash = (Flashcard) flashDrpDwn.getSelectedItem();
                        int flashId = selectedFlash.getFID();
                        int dictId = selectedFlash.getDict();
                        int index = flashDrpDwn.getSelectedItemPosition();

                        // Delete dictionary refresh drop-down menu content
                        localDB.deleteFlashcard(flashId);
                        populateFlashDropDown(dictId);

                        // Select preceding dict if there are dictionaries in DB or not first dict
                        if (flashDrpDwn.getCount() > 0 || index > 0) {
                            flashDrpDwn.setSelection(index - 1);
                        }
                    }
                });

        // Listener for negative button, dismiss alert
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    // Flips flashcard content
    public void flipFlashcard(View view) {
        Flashcard selectedFlash = (Flashcard) flashDrpDwn.getSelectedItem();
        String frontContent = selectedFlash.frontProperty();
        String backContent = selectedFlash.backProperty();

        if (flashContent.getText() == frontContent) {
            flashContent.setText(backContent);
        }

        else if (flashContent.getText() == backContent) {
            flashContent.setText(frontContent);
        }
    }

    // Selects previous flashcard
    public void prevFlashcard (View view) {
        int index = flashDrpDwn.getSelectedItemPosition();

        flashDrpDwn.setSelection(index - 1);
    }

    // Selects next flashcard
    public void nextFlashcard (View view) {
        int index = flashDrpDwn.getSelectedItemPosition();

        flashDrpDwn.setSelection(index + 1);
    }
}
