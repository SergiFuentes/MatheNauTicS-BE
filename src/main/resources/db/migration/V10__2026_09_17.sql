-- ============================================================
-- V10__security_hardening.sql
-- Token revocation (token_version) and game-session idempotency
-- ============================================================

-- JWT revocation: monotonically increasing version per user.
-- Default 0 is backward compatible with existing JWTs that do not
-- carry a tokenVersion claim (extracted as 0 by JwtService).
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

-- Idempotency: each /games/finish call carries a client-supplied
-- session token that is unique per logical game completion.
ALTER TABLE game_sessions
    ADD COLUMN IF NOT EXISTS session_token UUID;

UPDATE game_sessions
SET session_token = gen_random_uuid()
WHERE session_token IS NULL;

ALTER TABLE game_sessions
    ALTER COLUMN session_token SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'game_sessions'::regclass
          AND conname = 'game_sessions_session_token_unique'
    ) THEN
ALTER TABLE game_sessions
    ADD CONSTRAINT game_sessions_session_token_unique
        UNIQUE (session_token);
END IF;
END
$$;