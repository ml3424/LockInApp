package com.example.lockinapp;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private int currentPoints;
    private int totalStudyTime;
    private String displayName;
    private List<String> extraSubjects;

    public User() {} // empty constructor for firebase

    public User(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.currentPoints = 0;
        this.totalStudyTime = 0;
        this.extraSubjects = new ArrayList<>();
        extraSubjects.add("extra"); // for not removing the empty header in firebase
    }

    // Getters
    public String getUserId() {
        return userId;
    }
    public int getCurrentPoints() {
        return currentPoints;
    }
    public int getTotalStudyTime() {
        return totalStudyTime;
    }
    public String getDisplayName() {
        return displayName;
    }
    public List<String> getExtraSubjects() {
        return extraSubjects;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }
    public void setTotalStudyTime(int totalStudyTime) {
        this.totalStudyTime = totalStudyTime;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setExtraSubjects(List<String> extraSubjects) {
        this.extraSubjects = extraSubjects;
    }
}
