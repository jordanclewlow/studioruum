package gui;

public class Resource
{

	//unique identifiers for the resource and attached user
	public int resourceID, userID;

	//the type of resource, being the subclass of this
	public String resourceType;

	//the name given to the resource (not necessary)
	public String resourceName;

	//access level, which is private by default
	public String privacyLevel;

	// added id and name as constructor params
	public Resource(int id, String name)
	{

		resourceName = name;
		resourceID = id;

	}

	// Overloaded constructor used by Flashcard (no need for title)
	public Resource(int id)
	{
		resourceID = id;
	}

	// Getter and setter for resourceName
	public String getTitle()
	{

		return resourceName;

	}

	public int getResourceID()
	{

		return resourceID;

	}

	public void setTitle(String title)
	{

		this.resourceName = title;

	}

}