package gui;

import java.util.Vector;

public class Dictionary extends Resource
{

    // the collection of associated flashcards
    private Vector<Flashcard> deck;

    // the ID to identify a specific dictionary
    private int dictionaryID;

    public Dictionary(int dID, int rID, String name)
    {

        super(rID, name);
        dictionaryID = dID;
        //resourceType = "dictionary";
        //deck = new Vector<Flashcard>();

    }

    // Getter and setter for dictionaryID
    public int getDict()
    {

        return dictionaryID;

    }

    public void setDict(int ID)
    {

        this.dictionaryID = ID;

    }

}
