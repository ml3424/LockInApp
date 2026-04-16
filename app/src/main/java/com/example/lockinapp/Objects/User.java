package com.example.lockinapp.Objects;

import java.util.ArrayList;
import java.util.List;

/**
 * The type User.
 */
public class User {
    private String userId;
    private int currentPoints;
    private int totalStudyTime;
    private String displayName;
    private List<String> extraSubjects;

    /**
     * Default constructor required for Firebase
     */
    public User() {}

    /**
     * Initializes a new user with default values.
     *
     * @param userId      Unique identifier from Firebase Auth.
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

    /**
     * Gets user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets user id.
     *
     * @param userId the user id
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets current points.
     *
     * @return the current points
     */
    public int getCurrentPoints() {
        return currentPoints;
    }

    /**
     * Sets current points.
     *
     * @param currentPoints the current points
     */
    public void setCurrentPoints(int currentPoints) {
        this.currentPoints = currentPoints;
    }

    /**
     * Gets total study time.
     *
     * @return the total study time
     */
    public int getTotalStudyTime() {
        return totalStudyTime;
    }

    /**
     * Sets total study time.
     *
     * @param totalStudyTime the total study time
     */
    public void setTotalStudyTime(int totalStudyTime) {
        this.totalStudyTime = totalStudyTime;
    }

    /**
     * Gets display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets extra subjects.
     *
     * @return the extra subjects
     */
    public List<String> getExtraSubjects() {
        return extraSubjects;
    }

    /**
     * Sets extra subjects.
     *
     * @param extraSubjects the extra subjects
     */
    public void setExtraSubjects(List<String> extraSubjects) {
        this.extraSubjects = extraSubjects;
    }
}
