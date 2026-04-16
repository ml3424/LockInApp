package com.example.lockinapp.Objects;

public class Reward {
    private String rewardId;
    private String name;
    private int cost;
    private String description;
    private String imageUrl; // url to an external image

    /** Default constructor required for Firebase Realtime Database/Firestore. */
    public Reward() {}


    // --- Getters and Setters ---

    public String getRewardId() {
        return rewardId;
    }

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}