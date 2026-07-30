-- Enroll every student in every published exam (open catalog for student attempt flow)

INSERT INTO exam_enrollments (exam_id, student_id, enrolled_at)
SELECT e.id, u.id, NOW()
FROM exams e
CROSS JOIN users u
WHERE e.published = TRUE
  AND u.role = 'STUDENT'
ON CONFLICT (exam_id, student_id) DO NOTHING;

UPDATE exams e
SET enrolled_count = (
    SELECT COUNT(*)::int FROM exam_enrollments en WHERE en.exam_id = e.id
);
