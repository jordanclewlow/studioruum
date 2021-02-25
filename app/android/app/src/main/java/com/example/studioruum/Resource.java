package com.example.studioruum;

public class Resource {

    //unique identifiers for the resource and attached user
    public int resourceID;

    //the name given to the resource (not necessary)
    public String resourceName;

    // added id and name as constructor params
    public Resource(int id, String name) {
        resourceName = name;
        resourceID = id;
    }

    // Overloaded constructor used by Flashcard (no need for title)
    public Resource(int id) {
        resourceID = id;
    }

    // Getter and setter for resourceName
    public String getTitle() {
        return resourceName;
    }

    public void setTitle(String title) { this.resourceName = title; }

}