package com.example.lockinapp.Objects;

/**
 * The type Study session.
 */
public class StudySession {
    private String sessionId; // unique id for the session
    private String userId;
    private String subject;
    private String startTime;
    private long durationSeconds;
    private int aiConcentrationScore;
    private int pointsEarned;

    /**
     * Default constructor required for Firebase Database serialization.
     */
    public StudySession() {}

    /**
     * Initializes a new session record with full metrics.
     *
     * @param sessionId            Unique identifier for the session.
     * @param userId               Owner of the session.
     * @param subject              The topic of study.
     * @param startTime            ISO formatted timestamp.
     * @param durationSeconds      Total time elapsed in seconds.
     * @param aiConcentrationScore Final average focus score from Gemini.
     * @param pointsEarned         Gamification points calculated for this session.
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

    /**
     * Gets session id.
     *
     * @return the session id
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets session id.
     *
     * @param sessionId the session id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

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
     * Gets subject.
     *
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets subject.
     *
     * @param subject the subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Gets start time.
     *
     * @return the start time
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Sets start time.
     *
     * @param startTime the start time
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets duration seconds.
     *
     * @return the duration seconds
     */
    public long getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Sets duration seconds.
     *
     * @param durationSeconds the duration seconds
     */
    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /**
     * Gets ai concentration score.
     *
     * @return the ai concentration score
     */
    public int getAiConcentrationScore() {
        return aiConcentrationScore;
    }

    /**
     * Sets ai concentration score.
     *
     * @param aiConcentrationScore the ai concentration score
     */
    public void setAiConcentrationScore(int aiConcentrationScore) {
        this.aiConcentrationScore = aiConcentrationScore;
    }

    /**
     * Gets points earned.
     *
     * @return the points earned
     */
    public int getPointsEarned() {
        return pointsEarned;
    }

    /**
     * Sets points earned.
     *
     * @param pointsEarned the points earned
     */
    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }
}
