-- Lecturer student roster + proctoring session detail domain

CREATE TABLE IF NOT EXISTS lecturer_courses (
    id          BIGSERIAL PRIMARY KEY,
    lecturer_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_code VARCHAR(50)  NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_lecturer_courses UNIQUE (lecturer_id, course_code)
);

CREATE TABLE IF NOT EXISTS proctoring_sessions (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lecturer_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_code       VARCHAR(50)  NOT NULL,
    course_name       VARCHAR(255) NOT NULL,
    assessment_title  VARCHAR(255) NOT NULL,
    integrity_score   SMALLINT     NOT NULL CHECK (integrity_score BETWEEN 0 AND 100),
    duration_minutes  INT          NOT NULL,
    total_flags       INT          NOT NULL DEFAULT 0,
    device_flags      INT          NOT NULL DEFAULT 0,
    absence_flags     INT          NOT NULL DEFAULT 0,
    session_date      DATE         NOT NULL,
    started_at        TIME         NOT NULL,
    ended_at          TIME         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_proctoring_sessions_student ON proctoring_sessions (student_id);
CREATE INDEX IF NOT EXISTS idx_proctoring_sessions_lecturer ON proctoring_sessions (lecturer_id);

CREATE TABLE IF NOT EXISTS proctoring_session_events (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT       NOT NULL REFERENCES proctoring_sessions (id) ON DELETE CASCADE,
    event_time      TIME         NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    severity        VARCHAR(20)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    points_deducted SMALLINT,
    has_snapshot    BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      INT          NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_proctoring_session_events_session ON proctoring_session_events (session_id, sort_order);

-- Demo lecturer (uses existing password hash from STU-12345 when available)
INSERT INTO users (institutional_id, email, password, role, first_name, last_name, enabled, token_version)
SELECT 'STU-67890', 'lecturer.demo@university.edu', u.password, 'LECTURER', 'Demo', 'Lecturer', true, 0
FROM users u
WHERE u.institutional_id = 'STU-12345'
  AND NOT EXISTS (SELECT 1 FROM users existing WHERE existing.institutional_id = 'STU-67890');

-- Demo students
INSERT INTO users (institutional_id, email, password, role, first_name, last_name, enabled, token_version)
SELECT v.institutional_id, v.email, u.password, 'STUDENT', v.first_name, v.last_name, true, 0
FROM users u
CROSS JOIN (VALUES
    ('STU-902144', 'alex.mercer@university.edu', 'Alex', 'Mercer'),
    ('STU-902155', 'sarah.chen@university.edu', 'Sarah', 'Chen'),
    ('STU-902215', 'marcus.johnson@university.edu', 'Marcus', 'Johnson'),
    ('STU-902188', 'maria.garcia@university.edu', 'Maria', 'Garcia'),
    ('STU-902201', 'james.wilson@university.edu', 'James', 'Wilson')
) AS v(institutional_id, email, first_name, last_name)
WHERE u.institutional_id = 'STU-12345'
  AND NOT EXISTS (
      SELECT 1 FROM users existing WHERE existing.institutional_id = v.institutional_id
  );

-- Assign courses to all lecturers (including STU-67890 and any existing lecturer accounts)
INSERT INTO lecturer_courses (lecturer_id, course_code, course_name)
SELECT l.id, v.course_code, v.course_name
FROM users l
CROSS JOIN (VALUES
    ('MATH401', 'Advanced Calculus'),
    ('MATH202', 'Calculus'),
    ('CS101', 'Intro to CS')
) AS v(course_code, course_name)
WHERE l.role = 'LECTURER'
  AND NOT EXISTS (
      SELECT 1 FROM lecturer_courses lc
      WHERE lc.lecturer_id = l.id AND lc.course_code = v.course_code
  );

ALTER TABLE student_completed_assessments
    ALTER COLUMN created_at SET DEFAULT NOW();

-- Student assessment seeds (target averages match UI mockup)
INSERT INTO student_completed_assessments
    (student_id, course_name, course_code, assessment_type, category, taken_date, timing_type, start_time, end_time, submitted_time, integrity_score, status, created_at)
SELECT s.id, v.course_name, v.course_code, v.assessment_type, v.category, v.taken_date::date, v.timing_type,
       v.start_time::time, v.end_time::time, v.submitted_time::time, v.integrity_score, v.status, NOW()
FROM users s
CROSS JOIN (VALUES
    ('STU-902144', 'Advanced Calculus', 'MATH401', 'Quiz 1', 'math', '2026-07-20', 'TIMED', '09:00', '10:00', NULL, 40, 'UNDER_REVIEW'),
    ('STU-902144', 'Advanced Calculus', 'MATH401', 'Quiz 2', 'math', '2026-07-15', 'TIMED', '09:00', '10:00', NULL, 38, 'UNDER_REVIEW'),
    ('STU-902144', 'Advanced Calculus', 'MATH401', 'Midterm', 'math', '2026-07-10', 'TIMED', '09:00', '11:00', NULL, 45, 'UNDER_REVIEW'),
    ('STU-902144', 'Advanced Calculus', 'MATH401', 'Final Exam', 'math', '2026-07-05', 'TIMED', '09:00', '11:00', NULL, 45, 'UNDER_REVIEW'),
    ('STU-902155', 'Calculus', 'MATH202', 'Quiz 1', 'math', '2026-07-22', 'TIMED', '10:00', '11:00', NULL, 75, 'VERIFIED'),
    ('STU-902155', 'Calculus', 'MATH202', 'Quiz 2', 'math', '2026-07-18', 'TIMED', '10:00', '11:00', NULL, 78, 'VERIFIED'),
    ('STU-902155', 'Calculus', 'MATH202', 'Midterm', 'math', '2026-07-12', 'TIMED', '10:00', '12:00', NULL, 75, 'VERIFIED'),
    ('STU-902215', 'Intro to CS', 'CS101', 'Lab 1', 'cs', '2026-07-19', 'TIMED', '13:00', '14:00', NULL, 40, 'UNDER_REVIEW'),
    ('STU-902215', 'Intro to CS', 'CS101', 'Lab 2', 'cs', '2026-07-17', 'TIMED', '13:00', '14:00', NULL, 42, 'UNDER_REVIEW'),
    ('STU-902215', 'Intro to CS', 'CS101', 'Quiz 1', 'cs', '2026-07-14', 'TIMED', '13:00', '14:00', NULL, 44, 'UNDER_REVIEW'),
    ('STU-902215', 'Intro to CS', 'CS101', 'Quiz 2', 'cs', '2026-07-11', 'TIMED', '13:00', '14:00', NULL, 41, 'UNDER_REVIEW'),
    ('STU-902215', 'Intro to CS', 'CS101', 'Midterm', 'cs', '2026-07-08', 'TIMED', '13:00', '15:00', NULL, 43, 'UNDER_REVIEW'),
    ('STU-902188', 'Advanced Calculus', 'MATH401', 'Quiz 1', 'math', '2026-07-21', 'TIMED', '11:00', '12:00', NULL, 70, 'VERIFIED'),
    ('STU-902188', 'Advanced Calculus', 'MATH401', 'Midterm', 'math', '2026-07-16', 'TIMED', '11:00', '13:00', NULL, 72, 'VERIFIED'),
    ('STU-902201', 'Intro to CS', 'CS101', 'Lab 1', 'cs', '2026-07-23', 'TIMED', '14:00', '15:00', NULL, 95, 'VERIFIED'),
    ('STU-902201', 'Intro to CS', 'CS101', 'Lab 2', 'cs', '2026-07-21', 'TIMED', '14:00', '15:00', NULL, 94, 'VERIFIED'),
    ('STU-902201', 'Intro to CS', 'CS101', 'Quiz 1', 'cs', '2026-07-18', 'TIMED', '14:00', '15:00', NULL, 93, 'VERIFIED'),
    ('STU-902201', 'Intro to CS', 'CS101', 'Quiz 2', 'cs', '2026-07-15', 'TIMED', '14:00', '15:00', NULL, 96, 'VERIFIED'),
    ('STU-902201', 'Intro to CS', 'CS101', 'Midterm', 'cs', '2026-07-10', 'TIMED', '14:00', '16:00', NULL, 94, 'VERIFIED'),
    ('STU-902201', 'Intro to CS', 'CS101', 'Final Exam', 'cs', '2026-07-05', 'TIMED', '14:00', '16:00', NULL, 92, 'VERIFIED')
) AS v(institutional_id, course_name, course_code, assessment_type, category, taken_date, timing_type, start_time, end_time, submitted_time, integrity_score, status)
WHERE s.institutional_id = v.institutional_id
  AND NOT EXISTS (
      SELECT 1 FROM student_completed_assessments existing
      WHERE existing.student_id = s.id
        AND existing.course_code = v.course_code
        AND existing.assessment_type = v.assessment_type
  );

-- Alex Mercer proctoring session (detail page mockup)
INSERT INTO proctoring_sessions
    (student_id, lecturer_id, course_code, course_name, assessment_title, integrity_score, duration_minutes, total_flags, device_flags, absence_flags, session_date, started_at, ended_at, created_at)
SELECT stu.id, lec.id, 'MATH401', 'Advanced Calculus', 'Advanced Calculus Final', 42, 120, 12, 2, 1, '2026-07-05'::date, '09:00'::time, '11:00'::time, NOW()
FROM users stu
JOIN users lec ON lec.id = (
    SELECT id FROM users
    WHERE role = 'LECTURER'
    ORDER BY CASE WHEN institutional_id = 'STU-67890' THEN 0 ELSE 1 END, id
    LIMIT 1
)
WHERE stu.institutional_id = 'STU-902144'
  AND NOT EXISTS (
      SELECT 1 FROM proctoring_sessions ps
      WHERE ps.student_id = stu.id AND ps.assessment_title = 'Advanced Calculus Final'
  )
LIMIT 1;

INSERT INTO proctoring_session_events (session_id, event_time, event_type, severity, title, description, points_deducted, has_snapshot, sort_order)
SELECT ps.id, v.event_time::time, v.event_type, v.severity, v.title, v.description, v.points_deducted, v.has_snapshot, v.sort_order
FROM proctoring_sessions ps
JOIN users stu ON stu.id = ps.student_id AND stu.institutional_id = 'STU-902144'
CROSS JOIN (VALUES
    ('09:00', 'SESSION_STARTED', 'SUCCESS', 'Session Started', 'Identity verification successful. Environmental scan passed.', NULL, false, 1),
    ('09:45', 'MINOR_INFRACTION', 'WARNING', 'Minor Infraction (-2 pts)', 'Audio threshold exceeded. Background conversation detected.', 2, false, 2),
    ('10:12', 'CRITICAL_VIOLATION', 'DANGER', 'Critical Violation (-15 pts)', 'Primary subject left the camera frame for > 30 seconds. Empty desk snapshot captured at 10:12 AM.', 15, true, 3),
    ('10:40', 'CRITICAL_VIOLATION', 'DANGER', 'Critical Violation (-25 pts)', 'Secondary device (smartphone) detected in frame.', 25, false, 4),
    ('11:00', 'SESSION_ENDED', 'NEUTRAL', 'Session Ended', 'Exam submitted by user.', NULL, false, 5)
) AS v(event_time, event_type, severity, title, description, points_deducted, has_snapshot, sort_order)
WHERE ps.assessment_title = 'Advanced Calculus Final'
  AND NOT EXISTS (SELECT 1 FROM proctoring_session_events e WHERE e.session_id = ps.id);
