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

    // --- Getters and Setters ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }

    public int getTotalStudyTime() {
        return totalStudyTime;
    }

    public void setTotalStudyTime(int totalStudyTime) {
        this.totalStudyTime = totalStudyTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getExtraSubjects() {
        return extraSubjects;
    }

    public void setExtraSubjects(List<String> extraSubjects) {
        this.extraSubjects = extraSubjects;
    }
}
