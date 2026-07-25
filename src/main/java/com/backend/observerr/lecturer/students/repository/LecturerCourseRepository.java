package com.backend.observerr.lecturer.students.repository;

import com.backend.observerr.lecturer.students.model.LecturerCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LecturerCourseRepository extends JpaRepository<LecturerCourse, Long> {

    List<LecturerCourse> findByLecturerIdOrderByCourseCodeAsc(Long lecturerId);

    @Query(value = """
            SELECT u.id AS studentId,
                   u.institutional_id AS institutionalId,
                   u.first_name AS firstName,
                   u.last_name AS lastName,
                   lc.course_code AS courseCode,
                   lc.course_name AS courseName,
                   COUNT(sca.id) AS examsTaken,
                   AVG(sca.integrity_score) AS avgIntegrity,
                   MAX(sca.taken_date) AS lastActiveDate,
                   (
                       SELECT ps.id
                       FROM proctoring_sessions ps
                       WHERE ps.student_id = u.id
                         AND ps.lecturer_id = :lecturerId
                       ORDER BY ps.session_date DESC, ps.id DESC
                       LIMIT 1
                   ) AS latestSessionId
            FROM lecturer_courses lc
            JOIN student_completed_assessments sca ON sca.course_code = lc.course_code
            JOIN users u ON u.id = sca.student_id AND u.role = 'STUDENT'
            WHERE lc.lecturer_id = :lecturerId
              AND (:courseCode IS NULL OR lc.course_code = :courseCode)
              AND (
                    :search IS NULL
                    OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(u.institutional_id) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            GROUP BY u.id, u.institutional_id, u.first_name, u.last_name, lc.course_code, lc.course_name
            ORDER BY MAX(sca.taken_date) DESC, u.last_name ASC, u.first_name ASC
            """, nativeQuery = true)
    List<LecturerStudentRow> findRoster(
            @Param("lecturerId") Long lecturerId,
            @Param("courseCode") String courseCode,
            @Param("search") String search);
}
