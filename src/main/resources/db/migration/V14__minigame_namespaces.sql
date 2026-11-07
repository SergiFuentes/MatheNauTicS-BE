-- Add an explicit minigame namespace while preserving existing clients and data.
-- Legacy requests map to the "default" namespace until each frontend minigame is migrated.
ALTER TABLE game_session_tickets
    ADD COLUMN minigame_id VARCHAR(32) NOT NULL DEFAULT 'default';

ALTER TABLE game_sessions
    ADD COLUMN minigame_id VARCHAR(32) NOT NULL DEFAULT 'default';

ALTER TABLE player_progress
    ADD COLUMN minigame_id VARCHAR(32) NOT NULL DEFAULT 'default';

ALTER TABLE game_saves
    ADD COLUMN minigame_id VARCHAR(32) NOT NULL DEFAULT 'default';

ALTER TABLE player_progress
    DROP CONSTRAINT IF EXISTS player_progress_user_game_unique;

ALTER TABLE player_progress
    DROP CONSTRAINT IF EXISTS player_progress_pkey;

ALTER TABLE player_progress
    ADD CONSTRAINT player_progress_pkey
        PRIMARY KEY (user_id, game_mode, minigame_id);

ALTER TABLE game_session_tickets
    ADD CONSTRAINT game_session_tickets_minigame_id_check
        CHECK (minigame_id ~ '^[a-z0-9-]{1,32}$');

ALTER TABLE game_sessions
    ADD CONSTRAINT game_sessions_minigame_id_check
        CHECK (minigame_id ~ '^[a-z0-9-]{1,32}$');

ALTER TABLE player_progress
    ADD CONSTRAINT player_progress_minigame_id_check
        CHECK (minigame_id ~ '^[a-z0-9-]{1,32}$');

ALTER TABLE game_saves
    ADD CONSTRAINT game_saves_minigame_id_check
        CHECK (minigame_id ~ '^[a-z0-9-]{1,32}$');
