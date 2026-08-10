CREATE TABLE exam_questions (
    id            BIGSERIAL PRIMARY KEY,
    exam_id       BIGINT       NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    prompt        TEXT         NOT NULL,
    display_order INT          NOT NULL CHECK (display_order >= 0),
    points        INT          NOT NULL CHECK (points > 0),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_exam_questions_exam_order UNIQUE (exam_id, display_order)
);

CREATE INDEX idx_exam_questions_exam_order ON exam_questions (exam_id, display_order);

CREATE TABLE exam_question_options (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT       NOT NULL REFERENCES exam_questions (id) ON DELETE CASCADE,
    option_key     CHAR(1)      NOT NULL CHECK (option_key IN ('A', 'B', 'C', 'D')),
    option_text    TEXT         NOT NULL,
    correct_answer BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_exam_question_options_key UNIQUE (question_id, option_key)
);

CREATE UNIQUE INDEX uk_exam_question_options_one_correct
    ON exam_question_options (question_id) WHERE correct_answer = TRUE;

CREATE TABLE exam_answers (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          UUID         NOT NULL REFERENCES exam_sessions (id) ON DELETE CASCADE,
    question_id         BIGINT       NOT NULL REFERENCES exam_questions (id) ON DELETE CASCADE,
    selected_option_key CHAR(1)      NOT NULL CHECK (selected_option_key IN ('A', 'B', 'C', 'D')),
    submitted           BOOLEAN      NOT NULL DEFAULT FALSE,
    saved_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMP,
    CONSTRAINT uk_exam_answers_session_question UNIQUE (session_id, question_id)
);

CREATE INDEX idx_exam_answers_session ON exam_answers (session_id);

CREATE TABLE exam_results (
    id                 BIGSERIAL PRIMARY KEY,
    exam_id            BIGINT       NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    session_id         UUID         NOT NULL REFERENCES exam_sessions (id) ON DELETE CASCADE,
    student_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    lecturer_id        BIGINT       NOT NULL REFERENCES users (id),
    academic_score     INT          NOT NULL CHECK (academic_score >= 0),
    max_score          INT          NOT NULL CHECK (max_score > 0),
    integrity_score    SMALLINT     NOT NULL CHECK (integrity_score BETWEEN 0 AND 100),
    requires_review    BOOLEAN      NOT NULL DEFAULT FALSE,
    release_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (release_status IN ('PENDING', 'RELEASED')),
    submitted_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    released_at        TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_exam_results_session UNIQUE (session_id)
);

CREATE INDEX idx_exam_results_exam_status ON exam_results (exam_id, release_status);
CREATE INDEX idx_exam_results_student_status ON exam_results (student_id, release_status, submitted_at DESC);
