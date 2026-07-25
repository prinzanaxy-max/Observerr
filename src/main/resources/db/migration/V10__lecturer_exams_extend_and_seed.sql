-- Extend exams for lecturer create/list UI and seed demo data

ALTER TABLE exams ADD COLUMN IF NOT EXISTS course_code VARCHAR(50);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS course_name VARCHAR(255);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS duration_minutes INT;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS webcam_monitoring BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS tab_switch_tracking BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS block_copy_paste BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS published BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS enrolled_count INT NOT NULL DEFAULT 0;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS capacity_count INT;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS active_flags_count INT NOT NULL DEFAULT 0;

UPDATE exams
SET duration_minutes = COALESCE(duration_minutes, 120),
    course_code = COALESCE(course_code, 'GEN101'),
    course_name = COALESCE(course_name, title),
    published = COALESCE(published, TRUE)
WHERE duration_minutes IS NULL OR course_code IS NULL OR course_name IS NULL;

UPDATE exams
SET end_time = start_time + (duration_minutes || ' minutes')::interval
WHERE end_time IS NULL AND duration_minutes IS NOT NULL;

INSERT INTO exams
    (title, lecturer_id, status, start_time, end_time, duration_minutes, course_code, course_name,
     webcam_monitoring, tab_switch_tracking, block_copy_paste, published,
     enrolled_count, capacity_count, active_flags_count, start_notifications_sent, created_at, updated_at)
SELECT v.title, lec.id, v.status, v.start_time, v.end_time, v.duration_minutes, v.course_code, v.course_name,
       v.webcam_monitoring, v.tab_switch_tracking, v.block_copy_paste, TRUE,
       v.enrolled_count, v.capacity_count, v.active_flags_count, v.start_notifications_sent, NOW(), NOW()
FROM (
    SELECT id FROM users
    WHERE role = 'LECTURER'
    ORDER BY CASE WHEN institutional_id = 'STU-67890' THEN 0 ELSE 1 END, id
    LIMIT 1
) lec
CROSS JOIN (VALUES
    (
        'Advanced Calculus Final',
        'SCHEDULED',
        NOW() - INTERVAL '30 minutes',
        NOW() + INTERVAL '90 minutes',
        120,
        'MATH401',
        'Advanced Calculus',
        TRUE, TRUE, TRUE,
        145, 150, 3,
        FALSE
    ),
    (
        'Calculus Midterm',
        'SCHEDULED',
        DATE_TRUNC('day', NOW()) + INTERVAL '1 day 14 hours',
        DATE_TRUNC('day', NOW()) + INTERVAL '1 day 16 hours',
        120,
        'MATH202',
        'Calculus',
        TRUE, TRUE, TRUE,
        120, NULL, 0,
        FALSE
    ),
    (
        'Intro to CS Quiz',
        'SCHEDULED',
        DATE_TRUNC('day', NOW()) + INTERVAL '3 days 9 hours',
        DATE_TRUNC('day', NOW()) + INTERVAL '3 days 10 hours 30 minutes',
        90,
        'CS101',
        'Intro to CS',
        FALSE, TRUE, FALSE,
        85, NULL, 0,
        FALSE
    ),
    (
        'Advanced Calculus Midterm',
        'ENDED',
        DATE_TRUNC('day', NOW()) - INTERVAL '14 days 9 hours',
        DATE_TRUNC('day', NOW()) - INTERVAL '14 days 11 hours',
        120,
        'MATH401',
        'Advanced Calculus',
        TRUE, TRUE, TRUE,
        96, NULL, 0,
        TRUE
    ),
    (
        'Intro to CS Final',
        'ENDED',
        DATE_TRUNC('day', NOW()) - INTERVAL '30 days 13 hours',
        DATE_TRUNC('day', NOW()) - INTERVAL '30 days 15 hours',
        120,
        'CS101',
        'Intro to CS',
        TRUE, TRUE, TRUE,
        88, NULL, 0,
        TRUE
    )
) AS v(title, status, start_time, end_time, duration_minutes, course_code, course_name,
       webcam_monitoring, tab_switch_tracking, block_copy_paste,
       enrolled_count, capacity_count, active_flags_count, start_notifications_sent)
WHERE NOT EXISTS (
    SELECT 1 FROM exams e
    WHERE e.lecturer_id = lec.id AND e.title = v.title
);
