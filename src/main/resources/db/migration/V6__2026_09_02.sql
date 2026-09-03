ALTER TABLE player_progress
    ADD COLUMN IF NOT EXISTS game_mode VARCHAR(50);

UPDATE player_progress
SET game_mode = 'adventure'
WHERE game_mode IS NULL;

ALTER TABLE player_progress
    ALTER COLUMN game_mode SET NOT NULL;

ALTER TABLE player_progress
DROP CONSTRAINT IF EXISTS player_progress_user_id_key;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'player_progress'::regclass
          AND conname = 'player_progress_user_game_unique'
    ) THEN
ALTER TABLE player_progress
    ADD CONSTRAINT player_progress_user_game_unique
        UNIQUE (user_id, game_mode);
END IF;
END
$$;