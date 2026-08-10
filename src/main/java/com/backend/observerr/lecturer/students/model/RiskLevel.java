package com.backend.observerr.lecturer.students.model;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    /** LOW 71–100, MEDIUM 31–70, HIGH 0–30. */
    public static RiskLevel fromIntegrityScore(int avgIntegrity) {
        if (avgIntegrity >= 71) {
            return LOW;
        }
        if (avgIntegrity >= 31) {
            return MEDIUM;
        }
        return HIGH;
    }
}
