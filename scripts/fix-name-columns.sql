-- Quick fix for Railway crash:
-- Schema validation: missing column [first_name] in table [users]
--
-- Run this in Neon SQL Editor, then redeploy (or app will auto-fix with ddl-auto=update).

ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(50);

-- If you reset the users table manually but Flyway history is stale, reset Flyway too:
-- DELETE FROM flyway_schema_history WHERE version >= 2;
