-- Server-issued game-session tickets for staged migration away from
-- client-generated idempotency tokens. Finish enforcement is enabled only
-- after the frontend has been migrated to call POST /games/start.
CREATE TABLE game_session_tickets (
    session_token UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_mode VARCHAR(32) NOT NULL CHECK (game_mode IN ('adventure', 'training')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    CONSTRAINT game_session_tickets_expiry_after_start
        CHECK (expires_at > started_at)
);

CREATE INDEX game_session_tickets_user_started_idx
    ON game_session_tickets (user_id, started_at DESC);

CREATE INDEX game_session_tickets_expiry_idx
    ON game_session_tickets (expires_at)
    WHERE finished_at IS NULL;
