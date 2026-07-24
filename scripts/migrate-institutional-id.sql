-- Run once in Neon SQL editor before/after deploying institutional ID auth changes.

-- 1. Add institutional_id column
ALTER TABLE users ADD COLUMN IF NOT EXISTS institutional_id VARCHAR(255);

-- 2. Backfill existing users (adjust prefix if you prefer a different scheme)
UPDATE users
SET institutional_id = 'LEGACY-' || id::text
WHERE institutional_id IS NULL;

-- 3. Enforce constraints on institutional_id
ALTER TABLE users
  ALTER COLUMN institutional_id SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_institutional_id'
  ) THEN
    ALTER TABLE users ADD CONSTRAINT uk_users_institutional_id UNIQUE (institutional_id);
  END IF;
END $$;

-- 4. Remove full_name (no longer used)
ALTER TABLE users DROP COLUMN IF EXISTS full_name;
