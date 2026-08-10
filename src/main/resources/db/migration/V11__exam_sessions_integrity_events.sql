CREATE TABLE IF NOT EXISTS exam_sessions (
    id                     UUID PRIMARY KEY,
    exam_id                BIGINT       NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    student_id             BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    started_at             TIMESTAMP    NOT NULL,
    ended_at               TIMESTAMP,
    starting_score         SMALLINT     NOT NULL DEFAULT 100 CHECK (starting_score BETWEEN 0 AND 100),
    final_score            SMALLINT CHECK (final_score IS NULL OR final_score BETWEEN 0 AND 100),
    total_deductions       INT          NOT NULL DEFAULT 0,
    total_events           INT          NOT NULL DEFAULT 0,
    requires_review        BOOLEAN      NOT NULL DEFAULT FALSE,
    proctoring_available   BOOLEAN      NOT NULL DEFAULT TRUE,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_exam_sessions_exam_student ON exam_sessions (exam_id, student_id);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_status ON exam_sessions (status);

CREATE TABLE IF NOT EXISTS integrity_events (
    id               BIGSERIAL PRIMARY KEY,
    session_id       UUID         NOT NULL REFERENCES exam_sessions (id) ON DELETE CASCADE,
    client_event_id  UUID         NOT NULL UNIQUE,
    event_code       VARCHAR(80)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    severity         VARCHAR(20)  NOT NULL,
    points_deducted  INT          NOT NULL DEFAULT 0,
    score_after      INT          NOT NULL,
    requires_review  BOOLEAN      NOT NULL DEFAULT FALSE,
    occurred_at      TIMESTAMP    NOT NULL,
    duration_ms      INT,
    metadata         JSONB,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_integrity_events_session_occurred
    ON integrity_events (session_id, occurred_at);
