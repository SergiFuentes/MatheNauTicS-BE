ALTER TABLE player_progress DROP CONSTRAINT player_progress_pkey;

ALTER TABLE player_progress ADD PRIMARY KEY (user_id, game_mode);