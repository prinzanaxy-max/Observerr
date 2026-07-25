CREATE TABLE IF NOT EXISTS student_completed_assessments (
    id                  BIGSERIAL PRIMARY KEY,
    student_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_name         VARCHAR(255) NOT NULL,
    course_code         VARCHAR(50)  NOT NULL,
    assessment_type     VARCHAR(100) NOT NULL,
    category            VARCHAR(50)  NOT NULL,
    taken_date          DATE         NOT NULL,
    timing_type         VARCHAR(20)  NOT NULL,
    start_time          TIME,
    end_time            TIME,
    submitted_time      TIME,
    integrity_score     SMALLINT     NOT NULL CHECK (integrity_score BETWEEN 0 AND 100),
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_student_completed_assessments_student_date
    ON student_completed_assessments (student_id, taken_date DESC);

CREATE INDEX IF NOT EXISTS idx_student_completed_assessments_student_status
    ON student_completed_assessments (student_id, status);

-- Demo seed for the primary student account (STU-12345). Skips silently if that user does not exist yet.
INSERT INTO student_completed_assessments
    (student_id, course_name, course_code, assessment_type, category, taken_date, timing_type, start_time, end_time, submitted_time, integrity_score, status)
SELECT u.id, v.course_name, v.course_code, v.assessment_type, v.category, v.taken_date::date, v.timing_type,
       v.start_time::time, v.end_time::time, v.submitted_time::time, v.integrity_score, v.status
FROM users u
CROSS JOIN (VALUES
    ('Advanced Organic Chemistry', 'CHEM-401', 'Midterm Exam', 'science', '2023-10-12', 'TIMED', '09:00', '11:30', NULL, 98, 'VERIFIED'),
    ('Data Structures & Algorithms', 'CS-305', 'Quiz 4', 'cs', '2023-10-05', 'TIMED', '14:00', '14:45', NULL, 82, 'UNDER_REVIEW'),
    ('Linear Algebra', 'MATH-201', 'Final Exam', 'math', '2023-09-28', 'TIMED', '10:00', '13:00', NULL, 100, 'VERIFIED'),
    ('Modern World History', 'HIST-102', 'Essay Submission', 'history', '2023-09-15', 'SUBMITTED', NULL, NULL, '23:45', 95, 'VERIFIED'),
    ('Intro to Psychology', 'PSYC-101', 'Midterm Exam', 'psychology', '2023-09-08', 'TIMED', '11:00', '12:30', NULL, 91, 'VERIFIED'),
    ('Microeconomics', 'ECON-210', 'Quiz 2', 'economics', '2023-08-30', 'TIMED', '15:00', '15:45', NULL, 88, 'VERIFIED'),
    ('Operating Systems', 'CS-401', 'Final Exam', 'cs', '2023-08-22', 'TIMED', '09:00', '12:00', NULL, 76, 'UNDER_REVIEW'),
    ('Technical Writing', 'ENG-220', 'Portfolio Review', 'writing', '2023-08-14', 'SUBMITTED', NULL, NULL, '18:20', 97, 'VERIFIED'),
    ('Physics II', 'PHYS-202', 'Lab Practical', 'science', '2023-08-05', 'TIMED', '13:00', '15:00', NULL, 93, 'VERIFIED'),
    ('Ethics in Technology', 'PHIL-150', 'Final Essay', 'philosophy', '2023-07-28', 'SUBMITTED', NULL, NULL, '21:10', 99, 'VERIFIED'),
    ('Database Systems', 'CS-350', 'Midterm Exam', 'cs', '2023-07-20', 'TIMED', '10:00', '11:30', NULL, 85, 'VERIFIED'),
    ('Human Anatomy', 'BIO-301', 'Practical Exam', 'science', '2023-07-12', 'TIMED', '08:30', '10:30', NULL, 94, 'VERIFIED'),
    ('Discrete Mathematics', 'MATH-250', 'Quiz 3', 'math', '2023-07-03', 'TIMED', '14:00', '14:50', NULL, 79, 'UNDER_REVIEW'),
    ('Public Speaking', 'COMM-105', 'Recorded Presentation', 'communication', '2023-06-25', 'SUBMITTED', NULL, NULL, '19:00', 92, 'VERIFIED'),
    ('Software Engineering', 'CS-320', 'Group Project', 'cs', '2023-06-18', 'SUBMITTED', NULL, NULL, '17:30', 90, 'VERIFIED'),
    ('Calculus III', 'MATH-301', 'Final Exam', 'math', '2023-06-10', 'TIMED', '09:00', '12:00', NULL, 87, 'VERIFIED'),
    ('World Literature', 'LIT-210', 'Essay Exam', 'literature', '2023-06-02', 'TIMED', '13:00', '15:00', NULL, 96, 'VERIFIED'),
    ('Computer Networks', 'CS-340', 'Lab Exam', 'cs', '2023-05-25', 'TIMED', '10:00', '12:00', NULL, 81, 'UNDER_REVIEW'),
    ('Organic Chemistry II', 'CHEM-402', 'Quiz 6', 'science', '2023-05-18', 'TIMED', '11:00', '11:45', NULL, 94, 'VERIFIED'),
    ('Business Ethics', 'BUS-150', 'Case Study', 'business', '2023-05-10', 'SUBMITTED', NULL, NULL, '20:15', 88, 'VERIFIED'),
    ('Statistics for Engineers', 'STAT-220', 'Quiz 5', 'math', '2023-05-02', 'TIMED', '11:00', '11:45', NULL, 84, 'VERIFIED'),
    ('Mobile App Development', 'CS-430', 'Capstone Demo', 'cs', '2023-04-24', 'SUBMITTED', NULL, NULL, '14:30', 95, 'VERIFIED'),
    ('International Relations', 'POL-240', 'Midterm Exam', 'politics', '2023-04-16', 'TIMED', '09:30', '11:00', NULL, 78, 'UNDER_REVIEW'),
    ('Cloud Computing', 'CS-440', 'Final Exam', 'cs', '2023-04-08', 'TIMED', '10:00', '12:00', NULL, 89, 'VERIFIED')
) AS v(course_name, course_code, assessment_type, category, taken_date, timing_type, start_time, end_time, submitted_time, integrity_score, status)
WHERE u.institutional_id = 'STU-12345'
  AND NOT EXISTS (
      SELECT 1 FROM student_completed_assessments existing WHERE existing.student_id = u.id
  );
