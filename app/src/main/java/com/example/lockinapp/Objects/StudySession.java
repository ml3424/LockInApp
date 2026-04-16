package com.example.lockinapp.Objects;

public class StudySession {
    private String sessionId; // unique id for the session
    private String userId;
    private String subject;
    private String startTime;
    private long durationSeconds;
    private int aiConcentrationScore;
    private int pointsEarned;

    /** Default constructor required for Firebase Database serialization. */
    public StudySession() {}

    /**
     * Initializes a new session record with full metrics.
     * @param sessionId Unique identifier for the session.
     * @param userId Owner of the session.
     * @param subject The topic of study.
     * @param startTime ISO formatted timestamp.
     * @param durationSeconds Total time elapsed in seconds.
     * @param aiConcentrationScore Final average focus score from Gemini.
     * @param pointsEarned Gamification points calculated for this session.
     */
    public StudySession(String sessionId, String userId, String subject, String startTime,
                        long durationSeconds, int aiConcentrationScore, int pointsEarned) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.subject = subject;
        this.startTime = startTime;
        this.durationSeconds = durationSeconds;
        this.aiConcentrationScore = aiConcentrationScore;
        this.pointsEarned = pointsEarned;
    }

    // --- Getters and Setters ---

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getAiConcentrationScore() {
        return aiConcentrationScore;
    }

    public void setAiConcentrationScore(int aiConcentrationScore) {
        this.aiConcentrationScore = aiConcentrationScore;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }
}
