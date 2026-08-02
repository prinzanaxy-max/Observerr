package com.backend.observerr.lecturer.students.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LecturerStudentDto {

    private final Long id;
    private final String studentNumber;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final String initials;
    private final String courseCode;
    private final String courseName;
    private final String courseLabel;
    private final long examsTaken;
    private final int avgIntegrityScore;
    private final String riskLevel;
    private final String lastActive;
    private final String latestSessionId;
}
