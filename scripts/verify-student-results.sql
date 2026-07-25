-- Run manually in Neon SQL editor if Flyway has not applied V6–V8 yet.
-- Look for table: student_completed_assessments (there is no table named "results").

-- 1) Confirm the demo user exists
SELECT id, institutional_id, email FROM users WHERE institutional_id = 'STU-12345';

-- 2) Confirm Flyway migrations applied
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;

-- 3) Count seeded rows for STU-12345
SELECT COUNT(*) AS assessment_count
FROM student_completed_assessments sca
JOIN users u ON u.id = sca.student_id
WHERE u.institutional_id = 'STU-12345';

-- 4) Preview rows
SELECT sca.course_name, sca.course_code, sca.assessment_type, sca.integrity_score, sca.status
FROM student_completed_assessments sca
JOIN users u ON u.id = sca.student_id
WHERE u.institutional_id = 'STU-12345'
ORDER BY sca.taken_date DESC
LIMIT 10;
