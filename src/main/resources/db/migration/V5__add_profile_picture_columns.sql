ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(512),
    ADD COLUMN IF NOT EXISTS profile_picture_public_id VARCHAR(255);
