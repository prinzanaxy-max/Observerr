CREATE TABLE IF NOT EXISTS exams (
    id                          BIGSERIAL PRIMARY KEY,
    title                       VARCHAR(255) NOT NULL,
    lecturer_id                 BIGINT       NOT NULL REFERENCES users (id),
    status                      VARCHAR(50)  NOT NULL DEFAULT 'SCHEDULED',
    start_time                  TIMESTAMP    NOT NULL,
    end_time                    TIMESTAMP,
    start_notifications_sent    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_exams_status_start_time ON exams (status, start_time);
CREATE INDEX IF NOT EXISTS idx_exams_lecturer_id ON exams (lecturer_id);

CREATE TABLE IF NOT EXISTS exam_enrollments (
    id          BIGSERIAL PRIMARY KEY,
    exam_id     BIGINT      NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    student_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_exam_enrollments_exam_student UNIQUE (exam_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_exam_enrollments_exam_id ON exam_enrollments (exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_enrollments_student_id ON exam_enrollments (student_id);

CREATE TABLE IF NOT EXISTS device_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id ON device_tokens (user_id);
