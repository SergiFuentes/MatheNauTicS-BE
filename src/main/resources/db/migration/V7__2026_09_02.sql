ALTER TABLE player_progress
DROP CONSTRAINT IF EXISTS player_progress_game_mode_check;

ALTER TABLE player_progress
    ADD CONSTRAINT player_progress_game_mode_check
        CHECK (game_mode IN ('adventure', 'training'));