package com.example.lockinapp;

public class StudySession {
    public String sessionId; // unique id for the session
    public String userId;
    public String subject;
    public String startTime;
    public long durationSeconds;
    public int aiConcentrationScore;
    public int pointsEarned;

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
}
