-- Additional demo seed for STU-12345 (brings total to 40 completed assessments).
INSERT INTO student_completed_assessments
    (student_id, course_name, course_code, assessment_type, category, taken_date, timing_type, start_time, end_time, submitted_time, integrity_score, status)
SELECT u.id, v.course_name, v.course_code, v.assessment_type, v.category, v.taken_date::date, v.timing_type,
       v.start_time::time, v.end_time::time, v.submitted_time::time, v.integrity_score, v.status
FROM users u
CROSS JOIN (VALUES
    ('Artificial Intelligence', 'CS-450', 'Final Exam', 'cs', '2023-03-30', 'TIMED', '09:00', '12:00', NULL, 92, 'VERIFIED'),
    ('Macroeconomics', 'ECON-220', 'Midterm Exam', 'economics', '2023-03-22', 'TIMED', '10:00', '11:30', NULL, 86, 'VERIFIED'),
    ('Biochemistry', 'CHEM-310', 'Lab Report', 'science', '2023-03-15', 'SUBMITTED', NULL, NULL, '22:00', 94, 'VERIFIED'),
    ('Digital Logic Design', 'CS-280', 'Quiz 2', 'cs', '2023-03-08', 'TIMED', '14:00', '14:45', NULL, 77, 'UNDER_REVIEW'),
    ('Introduction to Sociology', 'SOC-101', 'Essay Submission', 'communication', '2023-02-28', 'SUBMITTED', NULL, NULL, '20:45', 91, 'VERIFIED'),
    ('Thermodynamics', 'PHYS-301', 'Midterm Exam', 'science', '2023-02-20', 'TIMED', '09:00', '11:00', NULL, 88, 'VERIFIED'),
    ('Financial Accounting', 'ACCT-201', 'Final Exam', 'business', '2023-02-12', 'TIMED', '13:00', '15:30', NULL, 83, 'VERIFIED'),
    ('Web Development', 'CS-310', 'Project Demo', 'cs', '2023-02-05', 'SUBMITTED', NULL, NULL, '16:15', 96, 'VERIFIED'),
    ('Environmental Science', 'ENV-110', 'Field Report', 'science', '2023-01-28', 'SUBMITTED', NULL, NULL, '18:30', 90, 'VERIFIED'),
    ('Criminal Justice', 'CJ-150', 'Case Analysis', 'law', '2023-01-20', 'SUBMITTED', NULL, NULL, '21:00', 85, 'VERIFIED'),
    ('Music Theory', 'MUS-120', 'Performance Exam', 'arts', '2023-01-12', 'TIMED', '11:00', '12:00', NULL, 74, 'UNDER_REVIEW'),
    ('Nutrition Science', 'NUTR-210', 'Quiz 4', 'science', '2023-01-05', 'TIMED', '15:00', '15:40', NULL, 89, 'VERIFIED'),
    ('Comparative Politics', 'POL-110', 'Midterm Exam', 'politics', '2022-12-18', 'TIMED', '10:00', '11:30', NULL, 80, 'UNDER_REVIEW'),
    ('Graphic Design', 'ART-205', 'Portfolio Review', 'arts', '2022-12-10', 'SUBMITTED', NULL, NULL, '19:20', 93, 'VERIFIED'),
    ('Research Methods', 'RES-300', 'Thesis Proposal', 'writing', '2022-12-02', 'SUBMITTED', NULL, NULL, '23:10', 98, 'VERIFIED'),
    ('Information Security', 'CS-460', 'Lab Practical', 'cs', '2022-11-24', 'TIMED', '08:30', '10:30', NULL, 87, 'VERIFIED')
) AS v(course_name, course_code, assessment_type, category, taken_date, timing_type, start_time, end_time, submitted_time, integrity_score, status)
WHERE u.institutional_id = 'STU-12345'
  AND NOT EXISTS (
      SELECT 1
      FROM student_completed_assessments existing
      WHERE existing.student_id = u.id
        AND existing.course_code = v.course_code
        AND existing.assessment_type = v.assessment_type
  );
