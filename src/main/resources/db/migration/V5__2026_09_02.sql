ALTER TABLE player_progress DROP CONSTRAINT IF EXISTS player_progress_user_id_key;

ALTER TABLE player_progress ADD CONSTRAINT player_progress_user_game_unique UNIQUE (user_id, game_mode);