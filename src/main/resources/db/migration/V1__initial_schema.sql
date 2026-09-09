-- ============================================================
-- V1__initial_schema.sql
-- Initial schema: Users, Game Sessions, and Player Progress
-- ============================================================

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
    );

-- 2. Game sessions table
-- Stores the final result of each completed match.
CREATE TABLE IF NOT EXISTS game_sessions (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_mode VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL CHECK (score >= 0),
    total_coins INTEGER NOT NULL CHECK (total_coins >= 0),
    duration_seconds INTEGER NOT NULL CHECK (duration_seconds >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    CONSTRAINT game_sessions_game_mode_check
    CHECK (game_mode IN ('adventure', 'training'))
    );

-- 3. Player progress table
-- Stores the current progress for each player and game mode.
CREATE TABLE IF NOT EXISTS player_progress (
                                               user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    game_mode VARCHAR(20) NOT NULL,
    current_level INTEGER NOT NULL DEFAULT 1 CHECK (current_level >= 1),
    current_table INTEGER DEFAULT 1,
    last_played_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
    );

-- 4. Performance indexes
CREATE INDEX IF NOT EXISTS idx_game_sessions_user_id
    ON game_sessions(user_id);

CREATE INDEX IF NOT EXISTS idx_game_sessions_score
    ON game_sessions(score DESC);

CREATE INDEX IF NOT EXISTS idx_game_sessions_game_mode
    ON game_sessions(game_mode);

CREATE INDEX IF NOT EXISTS idx_player_progress_user_id
    ON player_progress(user_id);

CREATE INDEX IF NOT EXISTS idx_player_progress_game_mode
    ON player_progress(game_mode);

-- 5. Leaderboard view
CREATE OR REPLACE VIEW leaderboard AS
SELECT
    u.username,
    gs.score,
    gs.total_coins,
    gs.created_at
FROM game_sessions gs
         JOIN users u ON gs.user_id = u.id
ORDER BY gs.score DESC;