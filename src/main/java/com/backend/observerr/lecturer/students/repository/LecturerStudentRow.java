package com.backend.observerr.lecturer.students.repository;

import java.time.LocalDate;

public interface LecturerStudentRow {

    Long getStudentId();

    String getInstitutionalId();

    String getFirstName();

    String getLastName();

    String getCourseCode();

    String getCourseName();

    Long getExamsTaken();

    Double getAvgIntegrity();

    LocalDate getLastActiveDate();

    String getLatestSessionId();
}
