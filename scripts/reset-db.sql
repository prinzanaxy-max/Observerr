-- Observerr: full DB reset for new auth schema
-- Run once in Neon → SQL Editor (deletes ALL users)

DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    institutional_id VARCHAR(255)  NOT NULL UNIQUE,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password        VARCHAR(255)  NOT NULL,
    role            VARCHAR(50)   NOT NULL,
    created_at      TIMESTAMP,
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    token_version   INTEGER       NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_institutional_id ON users (institutional_id);
CREATE INDEX idx_users_email ON users (email);
