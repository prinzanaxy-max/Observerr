package com.backend.observerr.lecturer.students.repository;

import com.backend.observerr.lecturer.students.model.LecturerCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query(value = """
            SELECT u.id AS studentId, u.institutional_id AS institutionalId,
                   u.first_name AS firstName, u.last_name AS lastName,
                   e.course_code AS courseCode, COALESCE(e.course_name, e.course_code) AS courseName,
                   COUNT(DISTINCT CASE WHEN es.status = 'COMPLETED' THEN es.id END) AS examsTaken,
                   COALESCE(AVG(COALESCE(er.integrity_score, es.final_score,
                       es.starting_score - es.total_deductions)), 100) AS avgIntegrity,
                   CAST(MAX(es.started_at) AS DATE) AS lastActiveDate,
                   (SELECT CAST(es2.id AS VARCHAR)
                    FROM exam_sessions es2 JOIN exams e2 ON e2.id = es2.exam_id
                    WHERE es2.student_id = u.id AND e2.lecturer_id = :lecturerId
                    ORDER BY es2.started_at DESC LIMIT 1) AS latestSessionId
            FROM exam_enrollments en
            JOIN exams e ON e.id = en.exam_id AND e.lecturer_id = :lecturerId
            JOIN users u ON u.id = en.student_id AND u.role = 'STUDENT'
            LEFT JOIN exam_sessions es ON es.exam_id = e.id AND es.student_id = u.id
            LEFT JOIN exam_results er ON er.session_id = es.id
            WHERE (:courseCode IS NULL OR e.course_code = :courseCode)
              AND (:search IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.institutional_id) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')))
                      LIKE LOWER(CONCAT('%', :search, '%')))
            GROUP BY u.id, u.institutional_id, u.first_name, u.last_name,
                     e.course_code, e.course_name
            ORDER BY MAX(es.started_at) DESC NULLS LAST, u.last_name, u.first_name
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT u.id, e.course_code
                FROM exam_enrollments en
                JOIN exams e ON e.id = en.exam_id AND e.lecturer_id = :lecturerId
                JOIN users u ON u.id = en.student_id AND u.role = 'STUDENT'
                WHERE (:courseCode IS NULL OR e.course_code = :courseCode)
                  AND (:search IS NULL OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(u.institutional_id) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')))
                          LIKE LOWER(CONCAT('%', :search, '%')))
                GROUP BY u.id, e.course_code
            ) roster
            """, nativeQuery = true)
    Page<LecturerStudentRow> findRealRoster(
            @Param("lecturerId") Long lecturerId,
            @Param("courseCode") String courseCode,
            @Param("search") String search,
            Pageable pageable);
}
