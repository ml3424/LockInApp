package com.example.lockinapp;

public class Reward {
    private String rewardId;
    private String name;
    private int cost;
    private String description;
    private String imageUrl; // url to an external image

    /** Default constructor required for Firebase Realtime Database/Firestore. */
    public Reward() {}


    public String getRewardId() {
        return rewardId;
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

    public String getImageUrl() {
        return imageUrl;
    }

}