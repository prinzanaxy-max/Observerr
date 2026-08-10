package com.backend.observerr.lecturer.students.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "lecturer_courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
