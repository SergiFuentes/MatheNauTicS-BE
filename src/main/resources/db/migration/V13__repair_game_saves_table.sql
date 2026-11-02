-- Repair installations where V12 was applied before the saved-games V11 migration.
-- IF NOT EXISTS keeps this compatible with databases where V11 already ran.
CREATE TABLE IF NOT EXISTS game_saves (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_mode VARCHAR(20) NOT NULL CHECK (game_mode IN ('adventure', 'training')),
    name VARCHAR(80) NOT NULL CHECK (char_length(trim(name)) BETWEEN 1 AND 80),
    state JSONB NOT NULL CHECK (jsonb_typeof(state) = 'object'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_game_saves_user_updated
    ON game_saves (user_id, updated_at DESC);
