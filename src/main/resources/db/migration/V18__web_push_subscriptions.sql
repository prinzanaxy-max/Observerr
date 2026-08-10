-- Replace FCM token strings with native Web Push subscriptions (endpoint + keys).
DELETE FROM device_tokens;

ALTER TABLE device_tokens DROP CONSTRAINT IF EXISTS device_tokens_token_key;
ALTER TABLE device_tokens DROP COLUMN IF EXISTS token;

ALTER TABLE device_tokens ADD COLUMN IF NOT EXISTS endpoint VARCHAR(2048) NOT NULL;
ALTER TABLE device_tokens ADD COLUMN IF NOT EXISTS p256dh VARCHAR(255) NOT NULL;
ALTER TABLE device_tokens ADD COLUMN IF NOT EXISTS auth VARCHAR(255) NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_device_tokens_endpoint ON device_tokens (endpoint);
