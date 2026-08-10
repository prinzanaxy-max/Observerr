-- Run once in Neon SQL editor if deploy failed with:
-- ERROR: column "token_version" of relation "users" contains null values

UPDATE users SET token_version = 0 WHERE token_version IS NULL;

ALTER TABLE users
  ALTER COLUMN token_version SET DEFAULT 0,
  ALTER COLUMN token_version SET NOT NULL;
