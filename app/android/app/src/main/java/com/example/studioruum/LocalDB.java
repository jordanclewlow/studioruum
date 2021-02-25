package com.example.studioruum;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.sql.ResultSet;
import java.util.*;

public class LocalDB extends SQLiteOpenHelper {
    private String sql = null;
    private SQLiteStatement stmt = null;

    public LocalDB(Context context) {
        super(context, "studioruum_db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creates resources table
        sql = "CREATE TABLE resources (" +
                "resource_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT" +
                ")";
        db.execSQL(sql);

        // Creates notes table
        sql = "CREATE TABLE notes (" +
                "note_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "resource_id INTEGER NOT NULL," +
                "note_title VARCHAR(32) NOT NULL," +
                "note_content VARCHAR(255) NOT NULL," +
                "FOREIGN KEY(resource_id) REFERENCES resources(resource_id)" +
                ")";
        db.execSQL(sql);

        // Creates dictionaries table
        sql = "CREATE TABLE dictionaries (" +
                "dictionary_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "resource_id INTEGER NOT NULL," +
                "dictionary_name VARCHAR(32) NOT NULL," +
                "FOREIGN KEY(resource_id) REFERENCES resources(resource_id)" +
                ")";
        db.execSQL(sql);

        // Creates flashcards table
        sql = "CREATE TABLE flashcards (" +
                "flashcard_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "resource_id INTEGER NOT NULL," +
                "dictionary_id INTEGER NOT NULL," +
                "front_content VARCHAR(255) NOT NULL," +
                "back_content VARCHAR(255) NOT NULL," +
                "FOREIGN KEY(dictionary_id) REFERENCES dictionaries(dictionary_id)," +
                "FOREIGN KEY(resource_id) REFERENCES resources(resource_id)" +
                ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drops resources table
        sql = "DROP TABLE IF EXISTS resources";
        db.execSQL(sql);

        // Drops notes table
        sql = "DROP TABLE IF EXISTS notes";
        db.execSQL(sql);

        // Drops dictionaries table
        sql = "DROP TABLE IF EXISTS dictionaries";
        db.execSQL(sql);

        // Drops flashcards table
        sql = "DROP TABLE IF EXISTS flashcards";
        db.execSQL(sql);

        // Create database from scratch
        onCreate(db);
    }

    public List allResources() {
        SQLiteDatabase localDB = this.getWritableDatabase();
        List<Hashtable> resourceList = new ArrayList<Hashtable>();

        try {
            sql = "SELECT * FROM resources";
            Cursor rs = localDB.rawQuery(sql, null);

            while (rs.moveToNext()) {
                Hashtable record = new Hashtable();

                record.put("resource_id", rs.getInt(rs.getColumnIndexOrThrow("resource_id")));

                resourceList.add(record);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return resourceList;
    }

    // Creates record for resource in local db
    // No values to be passed as params, as resource_id auto-increments
    public void saveResource() {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            sql = "INSERT INTO resources VALUES (null)";
            localDB.execSQL(sql);
        } catch (Exception e) {
            System.out.println("saveResource Error: " + e.getMessage());
        }
    }

    // Deletes resource record from local db
    // Needs a resource_id as a parameter to locate record to delete
    public void deleteResource(int resourceId) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            stmt = localDB.compileStatement("DELETE FROM resources WHERE resource_id = (?)");
            stmt.bindString(1, String.valueOf(resourceId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("deleteResource Error: " + e.getMessage());
        }
    }

    // Now returns list of com.example.studioruum.Note objects containing all notes in the local db
    public List allNotes() {
        SQLiteDatabase localDB = this.getWritableDatabase();
        List<Note> noteList = new ArrayList<>();

        try {
            sql = "SELECT * FROM notes";
            Cursor rs = localDB.rawQuery(sql, null);

            while (rs.moveToNext()) {
                // Fetch all values of current record
                int noteId = rs.getInt(rs.getColumnIndexOrThrow("note_id"));
                int resourceId = rs.getInt(rs.getColumnIndexOrThrow("resource_id"));
                String noteTitle = rs.getString(rs.getColumnIndexOrThrow("note_title"));
                String noteContent = rs.getString(rs.getColumnIndexOrThrow("note_content"));

                // Create com.example.studioruum.Note object and add to list
                Note record = new Note(noteId, resourceId, noteTitle, noteContent);
                noteList.add(record);
            }

        } catch (Exception e) {
            System.out.println("allNotes Error: " + e.getMessage());
        }

        return noteList;
    }

    // Saves note record in local db
    public void saveNote(String noteTitle, String noteContent) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            // Save as resource and retrieve automatically generated id value
            saveResource();
            sql = "SELECT MAX(resource_id) AS last_id FROM resources";
            Cursor rs = localDB.rawQuery(sql, null);
            int resourceId = 0;

            while (rs.moveToNext()) {
                resourceId = rs.getInt(rs.getColumnIndexOrThrow("last_id"));
            }

            // Save as note using retrieved resource_id
            stmt = localDB.compileStatement("INSERT INTO notes(note_id, resource_id, note_title, note_content) VALUES(null, ?, ?, ?)");
            stmt.bindString(1, String.valueOf(resourceId));
            stmt.bindString(2, noteTitle);
            stmt.bindString(3, noteContent);
            stmt.executeInsert();
        } catch (Exception e) {
            System.out.println("saveNote Error: " + e.getMessage());
        }
    }

    // Deletes note record from local db
    public void deleteNote(int noteId) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            // Retrieve corresponding resource_id for note to delete
            String[] columns = {"resource_id"};
            Cursor rs = localDB.query("notes", columns, "note_id = '" + noteId + "'", null, null, null, null);
            int resourceId = 0;

            while (rs.moveToNext()) {
                resourceId = rs.getInt(rs.getColumnIndexOrThrow("resource_id"));
            }

            // Delete record from resources table
            deleteResource(resourceId);

            // Delete record from notes table
            stmt = localDB.compileStatement("DELETE FROM notes WHERE note_id = (?)");
            stmt.bindString(1, String.valueOf(noteId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("deleteNote Error: " + e.getMessage());
        }
    }

    // Updates a note record from local db
    public void updateNote(int noteId, String noteTitle, String noteContent) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            stmt = localDB.compileStatement("UPDATE notes SET note_title = ?, note_content = ? WHERE note_id = (?)");
            stmt.bindString(1, noteTitle);
            stmt.bindString(2, noteContent);
            stmt.bindString(3, String.valueOf(noteId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("updateNote Error: " + e.getMessage());
        }
    }

    // Returns list of Dictionary containing all dictionaries in the local db
    public List allDictionaries() {
        SQLiteDatabase localDB = this.getWritableDatabase();
        List<Dictionary> dictionaryList = new ArrayList<Dictionary>();

        try {
            sql = "SELECT * FROM dictionaries";
            Cursor rs = localDB.rawQuery(sql, null);

            while (rs.moveToNext()) {
                // Fetch all values of current record
                int dictionaryId = rs.getInt(rs.getColumnIndexOrThrow("dictionary_id"));
                int resourceId = rs.getInt(rs.getColumnIndexOrThrow("resource_id"));
                String dictionaryName = rs.getString(rs.getColumnIndexOrThrow("dictionary_name"));

                // Create Dictionary object and add to list
                Dictionary record = new Dictionary(dictionaryId, resourceId, dictionaryName);
                dictionaryList.add(record);
            }
        } catch (Exception e) {
            System.out.println("allDictionaries Error: " + e.getMessage());
        }

        return dictionaryList;
    }

    public void saveDictionary(String dictionaryName) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            // Save as resource and retrieve automatically generated id value
            saveResource();
            sql = "SELECT MAX(resource_id) AS last_id FROM resources";
            Cursor rs = localDB.rawQuery(sql, null);
            int resourceId = 0;

            while (rs.moveToNext()) {
                resourceId = rs.getInt(rs.getColumnIndexOrThrow("last_id"));
            }

            // Save as dictionary using retrieved resource_id
            stmt = localDB.compileStatement("INSERT INTO dictionaries(resource_id, dictionary_name) VALUES(?, ?)");
            stmt.bindString(1, String.valueOf(resourceId));
            stmt.bindString(2, dictionaryName);
            stmt.executeInsert();
        } catch (Exception e) {
            System.out.println("saveDictionary Error: " + e.getMessage());
        }
    }

    public void deleteDictionary(int dictionaryId) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            // Retrieve corresponding resource_id for dictionary to delete
            String[] columns = {"resource_id"};
            Cursor rs = localDB.query("dictionaries", columns, "dictionary_id = '" + dictionaryId + "'", null, null, null, null);
            int resourceId = 0;

            while (rs.moveToNext()) {
                resourceId = rs.getInt(rs.getColumnIndexOrThrow("resource_id"));
            }

            // Delete record from resources table
            deleteResource(resourceId);

            // Delete all flashcards contained in dictionary
            stmt = localDB.compileStatement("DELETE FROM flashcards WHERE dictionary_id = (?)");
            stmt.bindString(1, String.valueOf(dictionaryId));
            stmt.executeUpdateDelete();

            // Delete dictionary
            stmt = localDB.compileStatement("DELETE FROM dictionaries WHERE dictionary_id = (?)");
            stmt.bindString(1, String.valueOf(dictionaryId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("deleteDictionary Error: " + e.getMessage());
        }
    }

    // Updates a dictionary record from local db
    public void updateDictionary(int dictId, String dictName) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            stmt = localDB.compileStatement("UPDATE dictionaries SET dictionary_name = ? WHERE dictionary_id = (?)");
            stmt.bindString(1, dictName);
            stmt.bindString(2, String.valueOf(dictId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("updateNote Error: " + e.getMessage());
        }
    }

    // Returns list of Flashcard containing all flashcards in dictionary passed as param
    public List allFlashcards(int dictID) {
        SQLiteDatabase localDB = this.getWritableDatabase();
        List<Flashcard> flashcardList = new ArrayList<Flashcard>();

        try {
            String[] columns = {"*"};
            Cursor rs = localDB.query("flashcards", columns, "dictionary_id = '" + dictID + "'", null, null, null, null);

            while (rs.moveToNext()) {
                // Fetch all values of current record
                int flashcardId = rs.getInt(rs.getColumnIndexOrThrow("flashcard_id"));
                int resourceId = rs.getInt(rs.getColumnIndexOrThrow("resource_id"));
                int dictionaryId = rs.getInt(rs.getColumnIndexOrThrow("dictionary_id"));
                String frontContent = rs.getString(rs.getColumnIndexOrThrow("front_content"));
                String backContent = rs.getString(rs.getColumnIndexOrThrow("back_content"));

                // Create Flashcard object and add to list
                Flashcard record = new Flashcard(flashcardId, resourceId, dictionaryId, frontContent, backContent);
                flashcardList.add(record);
            }
        } catch (Exception e) {
            System.out.println("allFlashcards Error: " + e.getMessage());
        }

        return flashcardList;
    }

    // Saves flashcard WITH quiz_id in local db
    public void saveFlashcard(int dictionaryId, String frontContent, String backContent) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            // Save as resource and retrieve automatically generated id value
            saveResource();
            sql = "SELECT MAX(resource_id) AS last_id FROM resources";
            Cursor rs = localDB.rawQuery(sql, null);
            int resourceId = 0;

            while (rs.moveToNext()) {
                resourceId = rs.getInt(rs.getColumnIndexOrThrow("last_id"));
            }

            // Save as dictionary using retrieved resource_id
            stmt = localDB.compileStatement("INSERT INTO flashcards(resource_id, dictionary_id, front_content, back_content) VALUES(?, ?, ?, ?)");
            stmt.bindString(1, String.valueOf(resourceId));
            stmt.bindString(2, String.valueOf(dictionaryId));
            stmt.bindString(3, frontContent);
            stmt.bindString(4, backContent);
            stmt.executeInsert();
        } catch (Exception e) {
            System.out.println("saveFlashcard Error: " + e.getMessage());
        }
    }

    public void deleteFlashcard(int flashcardId) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            // Retrieve corresponding resource_id for flashcard to delete
            String[] columns = {"resource_id"};
            Cursor rs = localDB.query("flashcards", columns, "flashcard_id = '" + flashcardId + "'", null, null, null, null);
            int resourceId = 0;

            while (rs.moveToNext()) {
                resourceId = rs.getInt(rs.getColumnIndexOrThrow("resource_id"));
            }

            // Delete record from resources table
            deleteResource(resourceId);

            // Delete flashcard from local db
            stmt = localDB.compileStatement("DELETE FROM flashcards WHERE flashcard_id = (?)");
            stmt.bindString(1, String.valueOf(flashcardId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("deleteFlashcard Error: " + e.getMessage());
        }
    }

    // Updates a flashcard record from local db
    public void updateFlashcard(int flashcardId, String frontContent, String backContent) {
        SQLiteDatabase localDB = this.getWritableDatabase();

        try {
            stmt = localDB.compileStatement("UPDATE flashcards SET front_content = (?), back_content = (?) WHERE flashcard_id = (?)");
            stmt.bindString(1, frontContent);
            stmt.bindString(2, backContent);
            stmt.bindString(3, String.valueOf(flashcardId));
            stmt.executeUpdateDelete();
        } catch (Exception e) {
            System.out.println("updateFlashcard Error: " + e.getMessage());
        }
    }

}
