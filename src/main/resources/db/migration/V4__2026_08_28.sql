ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_guest BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_users_username_lower
    ON users (LOWER(username));

CREATE INDEX IF NOT EXISTS idx_users_email_lower
    ON users (LOWER(email));

CREATE INDEX IF NOT EXISTS idx_users_is_guest
    ON users (is_guest);
