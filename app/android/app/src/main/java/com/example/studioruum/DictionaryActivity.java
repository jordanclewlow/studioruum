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
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DictionaryActivity extends AppCompatActivity {
    LocalDB localDB = new LocalDB(this);
    Spinner dictDrpDwn;
    Button renameDictBtn;
    Button deleteDictBtn;
    TableLayout dictTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);

        // Find elements in view
        dictDrpDwn = findViewById(R.id.dictDrpDwn);
        renameDictBtn = findViewById(R.id.renameDictBtn);
        deleteDictBtn = findViewById(R.id.deleteDictBtn);
        dictTable = findViewById(R.id.dictTable);

        // Populate dictionaries drop-down menu
        populateDropDown();
    }

    // Populates dictionaries drop down menu with data from local DB
    public void populateDropDown() {
        final List<Dictionary> dictionaries = localDB.allDictionaries();

        // If no dictionaries on DB, set drop-down element to "No dictionaries in DB"
        if (dictionaries.size() == 0) {
            Dictionary error = new Dictionary(0, 0, "No dictionaries in DB");
            List<Dictionary> noDicts = new ArrayList<Dictionary>();
            noDicts.add(error);
            ArrayAdapter<Dictionary> noteAdapter = new ArrayAdapter<Dictionary>(this, android.R.layout.simple_spinner_item, noDicts);
            noteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dictDrpDwn.setAdapter(noteAdapter);
            Toast toast = Toast.makeText(DictionaryActivity.this, "No dictionaries in DB", Toast.LENGTH_SHORT);
            toast.show();
        }

        // If dictionaries in DB, populate drop down
        else {
            ArrayAdapter<Dictionary> dictAdapter = new ArrayAdapter<Dictionary>(this, android.R.layout.simple_spinner_item, dictionaries);
            dictAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dictDrpDwn.setAdapter(dictAdapter);

            // Set listener for item selection from drop-down
            dictDrpDwn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Get selected dictionary and id
                    Dictionary selected = (Dictionary) parent.getSelectedItem();
                    int dictId = selected.getDict();

                    // If dictionaries in DB, populate table with flashcards from selected dict
                    if (selected.getTitle() != "No dictionaries in DB" && selected.resourceID != 0) {
                        List <Flashcard> fromSelected = localDB.allFlashcards(dictId);

                        // If dictionary contains flashcards, populate table
                        if (!fromSelected.isEmpty()) {
                            // Iterates through Flashcards from selected dict
                            for (int i = 0; i < fromSelected.size(); i++) {
                                // Get front and back of current flashcard
                                Flashcard flashcard = fromSelected.get(i);
                                String frontContent = flashcard.frontProperty();
                                String backContent = flashcard.backProperty();

                                // Create row element and set layout parameters
                                TableRow row = new TableRow(DictionaryActivity.this);
                                row.setLayoutParams(new TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT));

                                // Create front text view, set layout parameters and text
                                TextView labelFront = new TextView(DictionaryActivity.this);
                                TableRow.LayoutParams paramsFront = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT,1.0f);
                                labelFront.setText(frontContent);
                                labelFront.setWidth(0);
                                labelFront.setLayoutParams(paramsFront);
                                labelFront.setPadding(5, 5, 5, 5);
                                row.addView(labelFront);

                                // Create back text view, set layout parameters and text
                                TextView labelBack = new TextView(DictionaryActivity.this);
                                TableRow.LayoutParams paramsBack = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT,1.0f);
                                labelBack.setText(backContent);
                                labelBack.setWidth(0);
                                labelBack.setLayoutParams(paramsBack);
                                labelBack.setPadding(5, 5, 5, 5);
                                row.addView(labelBack);

                                // Add row to table
                                dictTable.addView(row, new TableLayout.LayoutParams(
                                        TableLayout.LayoutParams.FILL_PARENT,
                                        TableLayout.LayoutParams.MATCH_PARENT));
                            }
                        }

                        // If no flashcards in dictionary, alert user
                        else {
                            // Create and display alert
                            AlertDialog alertDialog = new AlertDialog.Builder(DictionaryActivity.this).create();
                            alertDialog.setTitle("No Flashcards");
                            alertDialog.setMessage("There are no flashcards in the dictionary you selected, do you want to create them?");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(DictionaryActivity.this, FlashcardActivity.class);
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
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    // Saves new dictionary in local DB
    public void createDictionary (View view) {
        // Create alert dialog and text input
        AlertDialog alertDialog = new AlertDialog.Builder(DictionaryActivity.this).create();
        final EditText input = new EditText(this);

        // Set title and add text input to view
        alertDialog.setTitle("Create a new Dictionary");
        alertDialog.setView(input);

        // Listener for positive button, create dictionary
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newDictName = String.valueOf(input.getText());
                localDB.saveDictionary(newDictName);
                populateDropDown();
                dictDrpDwn.setSelection(dictDrpDwn.getCount() -1);
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

    //Renames a dictionary in local DB
    public void renameDictionary (View v) {
        // Get selected dictionary info
        Dictionary selected = (Dictionary) dictDrpDwn.getSelectedItem();
        final int id = selected.getDict();
        String name = selected.getTitle();
        final int index = dictDrpDwn.getSelectedItemPosition();

        // Create alert dialog and text input
        AlertDialog alertDialog = new AlertDialog.Builder(DictionaryActivity.this).create();
        final EditText input = new EditText(this);

        // Set title, add text input to view
        alertDialog.setTitle("Rename Dictionary");
        input.setText(name);
        alertDialog.setView(input);

        // Listener for positive button, rename dictionary
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String editedDictName = String.valueOf(input.getText());
                localDB.updateDictionary(id, editedDictName);
                populateDropDown();
                dictDrpDwn.setSelection(index);
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

    // Deletes dictionary from local DB
    public void deleteDictionary (View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(DictionaryActivity.this).create();
        alertDialog.setTitle("Deletion Alert");
        alertDialog.setMessage("Are you sure you want to delete this dictionary?");

        // Listener for positive button, delete dictionary
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Get id and index of selected dictionary
                        Dictionary selected = (Dictionary) dictDrpDwn.getSelectedItem();
                        int id = selected.getDict();
                        int index = dictDrpDwn.getSelectedItemPosition();

                        // Delete dictionary refresh drop-down menu content
                        localDB.deleteDictionary(id);
                        populateDropDown();

                        // Select preceding dict if there are dictionaries in DB or not first dict
                        if (localDB.allDictionaries().size() > 0 || index > 0) {
                            dictDrpDwn.setSelection(index - 1);
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
}
