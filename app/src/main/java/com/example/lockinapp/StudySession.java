package com.example.lockinapp;

public class StudySession {
    public String sessionId; // unique id for the session
    public String userId;
    public String subject;
    public String startTime;
    public long durationSeconds;
    public int aiConcentrationScore;
    public int pointsEarned;

    public StudySession() {}

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
