CREATE TABLE IF NOT EXISTS users (
    id               BIGSERIAL PRIMARY KEY,
    institutional_id VARCHAR(255) NOT NULL UNIQUE,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    role             VARCHAR(50)  NOT NULL,
    first_name       VARCHAR(50),
    last_name        VARCHAR(50),
    created_at       TIMESTAMP,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    token_version    INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_users_institutional_id ON users (institutional_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
