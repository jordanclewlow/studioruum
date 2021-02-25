package com.example.studioruum;

public class Flashcard extends Resource {

    //the text kept on each side of the flashcard
    private String frontContent;
    private String backContent;

    // Added flashcardID
    private int flashcardID, dictionaryID;

    // Added new parameters
    public Flashcard(int fID, int rID, int dID, String front, String back)
    {
        super(rID);
        flashcardID = fID;
        dictionaryID = dID;
        frontContent = front;
        backContent = back;
    }

    // getter and setter for flashcardID
    public int getFID() {
        return flashcardID;
    }

    public void setFID(int ID) {
        this.flashcardID = ID;
    }

    // getter and setter for frontContent
    public String frontProperty() {
        return frontContent;
    }

    public void setFront(String content) {
        this.frontContent = content;
    }

    // getter and setter for backContent
    public String backProperty() {
        return backContent;
    }

    public void setBack(String content) {
        this.backContent = content;
    }

    // getter and setter for dictionaryID
    public int getDict() {
        return dictionaryID;
    }

    public void setDict(int ID) {
        this.dictionaryID = ID;
    }

    @Override
    public String toString() {
        return frontProperty();
    }
}