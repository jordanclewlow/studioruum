package com.example.studioruum;

public class Dictionary extends Resource {

    // the ID to identify a specific dictionary
    private int dictionaryID;

    public Dictionary(int dID, int rID, String name) {
        super(rID, name);
        dictionaryID = dID;
    }

    // Getter and setter for dictionaryID
    public int getDict() {
        return dictionaryID;
    }

    public void setDict(int ID) {
        this.dictionaryID = ID;
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
