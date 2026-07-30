-- Demo enrollments + live exam sessions for lecturer dashboard / live monitoring (STU-67890)

INSERT INTO exam_enrollments (exam_id, student_id, enrolled_at)
SELECT e.id, u.id, NOW()
FROM exams e
JOIN users l ON l.id = e.lecturer_id AND l.institutional_id = 'STU-67890'
JOIN users u ON u.institutional_id IN (
    'STU-12345', 'STU-902144', 'STU-902155', 'STU-902215', 'STU-902188', 'STU-902201'
)
WHERE e.title = 'Advanced Calculus Final'
ON CONFLICT (exam_id, student_id) DO NOTHING;

INSERT INTO exam_sessions (
    id, exam_id, student_id, started_at, ended_at,
    starting_score, final_score, total_deductions, total_events,
    requires_review, proctoring_available, status, created_at, updated_at
)
SELECT v.session_id, e.id, u.id, NOW() - INTERVAL '45 minutes', NULL,
       100, NULL, v.deductions, v.events,
       v.requires_review, TRUE, 'IN_PROGRESS', NOW(), NOW()
FROM exams e
JOIN users l ON l.id = e.lecturer_id AND l.institutional_id = 'STU-67890'
CROSS JOIN (VALUES
    ('550e8400-e29b-41d4-a716-446655440001'::uuid, 'STU-902144', 58, 4, TRUE, 42),
    ('550e8400-e29b-41d4-a716-446655440002'::uuid, 'STU-902155', 12, 2, FALSE, 88),
    ('550e8400-e29b-41d4-a716-446655440003'::uuid, 'STU-902215', 25, 3, TRUE, 75),
    ('550e8400-e29b-41d4-a716-446655440004'::uuid, 'STU-12345', 5, 1, FALSE, 95)
) AS v(session_id, institutional_id, deductions, events, requires_review, score_after)
JOIN users u ON u.institutional_id = v.institutional_id
WHERE e.title = 'Advanced Calculus Final'
ON CONFLICT (id) DO NOTHING;

INSERT INTO integrity_events (
    session_id, client_event_id, event_code, title, description, severity,
    points_deducted, score_after, requires_review, occurred_at, duration_ms, metadata
)
SELECT s.id, gen_random_uuid(), v.event_code, v.title, v.description, v.severity,
       v.points_deducted, v.score_after, v.requires_review, NOW() - v.ago, v.duration_ms, '{}'::jsonb
FROM exam_sessions s
JOIN exams e ON e.id = s.exam_id
JOIN users l ON l.id = e.lecturer_id AND l.institutional_id = 'STU-67890'
JOIN users st ON st.id = s.student_id
JOIN (VALUES
    ('STU-902144', 'TAB_BLUR', 'Tab switched away', 'Student left exam tab', 'HIGH', 15, 42, TRUE, INTERVAL '2 minutes', 120000),
    ('STU-902155', 'FACE_NOT_VISIBLE', 'Face not visible', 'Webcam obstructed briefly', 'MEDIUM', 5, 88, FALSE, INTERVAL '8 minutes', 3000),
    ('STU-902215', 'MULTIPLE_FACES', 'Multiple faces detected', 'Secondary face in frame', 'HIGH', 10, 75, TRUE, INTERVAL '5 minutes', 45000)
) AS v(institutional_id, event_code, title, description, severity, points_deducted, score_after, requires_review, ago, duration_ms)
    ON st.institutional_id = v.institutional_id
WHERE e.title = 'Advanced Calculus Final'
  AND NOT EXISTS (
      SELECT 1 FROM integrity_events ie WHERE ie.session_id = s.id AND ie.event_code = v.event_code
  );

UPDATE exams e
SET enrolled_count = (
    SELECT COUNT(*) FROM exam_enrollments en WHERE en.exam_id = e.id
)
FROM users l
WHERE e.lecturer_id = l.id
  AND l.institutional_id = 'STU-67890'
  AND e.title = 'Advanced Calculus Final';
