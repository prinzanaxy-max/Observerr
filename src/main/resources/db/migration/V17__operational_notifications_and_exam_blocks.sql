CREATE TABLE notification_preferences (
    user_id          BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    exam_events      BOOLEAN NOT NULL DEFAULT TRUE,
    integrity_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    result_updates   BOOLEAN NOT NULL DEFAULT TRUE,
    system_updates   BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE notifications (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category          VARCHAR(20) NOT NULL
        CHECK (category IN ('EXAM', 'INTEGRITY', 'RESULT', 'SYSTEM')),
    title             VARCHAR(255) NOT NULL,
    message           TEXT NOT NULL,
    deep_link         VARCHAR(1000),
    deduplication_key VARCHAR(255),
    read_at           TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_notifications_user_dedupe UNIQUE (user_id, deduplication_key)
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread
    ON notifications (user_id, read_at, created_at DESC);

CREATE TABLE exam_student_blocks (
    id            BIGSERIAL PRIMARY KEY,
    exam_id       BIGINT NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    student_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_by    BIGINT NOT NULL REFERENCES users (id),
    reason        VARCHAR(500),
    blocked_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    unblocked_at  TIMESTAMP,
    CONSTRAINT uk_exam_student_blocks UNIQUE (exam_id, student_id)
);

CREATE INDEX idx_exam_student_blocks_active
    ON exam_student_blocks (exam_id, student_id, unblocked_at);

WITH duplicate_active AS (
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY exam_id, student_id ORDER BY started_at DESC, id DESC
    ) AS attempt_rank
    FROM exam_sessions
    WHERE status = 'IN_PROGRESS'
)
UPDATE exam_sessions es
SET status = 'COMPLETED',
    ended_at = COALESCE(es.ended_at, NOW()),
    final_score = COALESCE(es.final_score,
        GREATEST(0, es.starting_score - es.total_deductions)),
    requires_review = TRUE,
    updated_at = NOW()
FROM duplicate_active duplicate
WHERE es.id = duplicate.id AND duplicate.attempt_rank > 1;

CREATE UNIQUE INDEX uk_exam_sessions_active_attempt
    ON exam_sessions (exam_id, student_id) WHERE status = 'IN_PROGRESS';
CREATE INDEX idx_exam_sessions_exam_status_updated
    ON exam_sessions (exam_id, status, updated_at DESC);
CREATE INDEX idx_integrity_events_occurred_session
    ON integrity_events (occurred_at DESC, session_id);
CREATE INDEX idx_exam_results_lecturer_submitted
    ON exam_results (lecturer_id, submitted_at DESC);
