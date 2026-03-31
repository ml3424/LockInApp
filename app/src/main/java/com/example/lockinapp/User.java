package com.example.lockinapp;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private int currentPoints;
    private int totalStudyTime;
    private String displayName;
    private List<String> extraSubjects;

    /** Default constructor required for Firebase*/
    public User() {}

    /**
     * Initializes a new user with default values.
     * @param userId Unique identifier from Firebase Auth.
     * @param displayName The user's name.
     */
    public User(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.currentPoints = 0;
        this.totalStudyTime = 0;
        this.extraSubjects = new ArrayList<>();
        extraSubjects.add("extra"); // for not removing the empty header in firebase
    }
}
