-- DEPRECATED: use scripts/reset-db.sql instead.
-- Incremental migration for databases that must keep existing user rows.

ALTER TABLE users ADD COLUMN IF NOT EXISTS institutional_id VARCHAR(255);

UPDATE users
SET institutional_id = 'LEGACY-' || id::text
WHERE institutional_id IS NULL;

ALTER TABLE users ALTER COLUMN institutional_id SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_institutional_id'
  ) THEN
    ALTER TABLE users ADD CONSTRAINT uk_users_institutional_id UNIQUE (institutional_id);
  END IF;
END $$;

ALTER TABLE users DROP COLUMN IF EXISTS full_name;
