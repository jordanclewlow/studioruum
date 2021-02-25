package gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Note extends Resource
{

    private int noteID;

    //Replaced by Resource.resourceName
    //private String noteTitle;

	//this will be what the user writes
    private String noteContent;

	// Constructor now accepts note_id, resource_id, title and content as params
    public Note(int nID, int rID, String title, String content)
    {

    	super(rID, title);
		noteID = nID;
		noteContent = content;

        //example data 
        //noteContent = "Need to research x by date y";

    }


    // getter and setter for noteID
	public int getDict()
	{

		return noteID;

	}


	// getter and setter for noteContent
	public String getContent()
	{

		return noteContent;

	}

	public void setContent(String content)
	{

		this.noteContent = content;

	}

}