-- Lecturers opt in to retakes; default is one attempt per student per exam.
ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS allow_retake BOOLEAN NOT NULL DEFAULT FALSE;
