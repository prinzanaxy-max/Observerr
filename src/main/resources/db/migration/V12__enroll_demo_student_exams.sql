-- Enroll demo student in published exams for student exam list/detail APIs

INSERT INTO exam_enrollments (exam_id, student_id)
SELECT e.id, u.id
FROM exams e
JOIN users u ON u.institutional_id = 'STU-12345'
WHERE e.published = TRUE
ON CONFLICT (exam_id, student_id) DO NOTHING;
