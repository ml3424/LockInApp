package com.example.lockinapp;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int userId;
    private int currentPoints;
    private int totalStudyTime;
    private String displayName;
    private List<String> extraSubjects;

    public User() {} // empty constructor for firebase

    public User(int userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.currentPoints = 0;
        this.totalStudyTime = 0;
        this.extraSubjects = new ArrayList<>();
        extraSubjects.add("extra"); // for not removing the empty header in firebase
    }
}
