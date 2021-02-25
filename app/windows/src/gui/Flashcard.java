package gui;

import javafx.beans.property.SimpleStringProperty;

//a flashcard is a type of resource
public class Flashcard extends Resource
{

	//the text kept on each side of the flashcard
	//changed to SimpleStringProperty in order to populate TableView
	private SimpleStringProperty frontContent;
	private SimpleStringProperty backContent;

	//the ID for the flashcard and the dictionary it is within
	private int flashcardID, dictionaryID;
	private int quizID = 0;

	//constructor to make a flashcard
	public Flashcard(int fID, int rID, int dID, String front, String back)
	{

		super(rID);
		flashcardID = fID;
		dictionaryID = dID;
		frontContent = new SimpleStringProperty(front);
		backContent = new SimpleStringProperty(back);

	}

	//overloaded constructor in case quizID passed as param
	public Flashcard(int fID, int rID, int dID, int qID, String front, String back)
	{

		super(rID);
		flashcardID = fID;
		dictionaryID = dID;
		quizID = qID;
		frontContent = new SimpleStringProperty(front);
		backContent = new SimpleStringProperty(back);

	}

	// getter and setter for flashcardID
	public int getFID()
	{

		return flashcardID;

	}

	public void setFID(int ID)
	{

		this.flashcardID = ID;

	}

	// getter and setter for frontContent
	public SimpleStringProperty frontProperty()
	{

		return frontContent;

	}

	public void setFront(String content)
	{

		this.frontContent = new SimpleStringProperty(content);

	}

	// getter and setter for backContent
	public SimpleStringProperty backProperty()
	{

		return backContent;

	}

	public void setBack(String content)
	{

		this.backContent = new SimpleStringProperty(content);

	}

	// getter and setter for dictionaryID
	public int getDict()
	{

		return dictionaryID;

	}

	public void setDict(int ID)
	{

		this.dictionaryID = ID;

	}

	// getter and setter for quizID
	public int getQuiz()
	{

		return quizID;

	}

	public void setQuiz(int ID)
	{

		this.quizID = ID;

	}


}
