package com.backend.observerr.lecturer.students.model;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static RiskLevel fromIntegrityScore(int avgIntegrity) {
        if (avgIntegrity >= 85) {
            return LOW;
        }
        if (avgIntegrity >= 60) {
            return MEDIUM;
        }
        return HIGH;
    }
}
