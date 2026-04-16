package com.example.lockinapp.Objects;

/**
 * The type Reward.
 */
public class Reward {
    private String rewardId;
    private String name;
    private int cost;
    private String description;
    private String imageUrl; // url to an external image

    /**
     * Default constructor required for Firebase Realtime Database/Firestore.
     */
    public Reward() {}


    // --- Getters and Setters ---

    /**
     * Gets reward id.
     *
     * @return the reward id
     */
    public String getRewardId() {
        return rewardId;
    }

    /**
     * Sets reward id.
     *
     * @param rewardId the reward id
     */
    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets cost.
     *
     * @return the cost
     */
    public int getCost() {
        return cost;
    }

    /**
     * Sets cost.
     *
     * @param cost the cost
     */
    public void setCost(int cost) {
        this.cost = cost;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets image url.
     *
     * @return the image url
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets image url.
     *
     * @param imageUrl the image url
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}