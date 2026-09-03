-- Add columns to store full game state in player_progress
ALTER TABLE player_progress
    ADD COLUMN score INT DEFAULT 0,
    ADD COLUMN lives INT DEFAULT 3,
    ADD COLUMN coins INT DEFAULT 0,
    ADD COLUMN difficulty VARCHAR(20) DEFAULT 'normal';

-- Update existing rows to have default values
UPDATE player_progress
SET score = 0, lives = 3, coins = 0, difficulty = 'normal'
WHERE score IS NULL OR lives IS NULL OR coins IS NULL OR difficulty IS NULL;

-- Make columns NOT NULL after setting defaults
ALTER TABLE player_progress
    ALTER COLUMN score SET NOT NULL,
ALTER COLUMN lives SET NOT NULL,
    ALTER COLUMN coins SET NOT NULL,
    ALTER COLUMN difficulty SET NOT NULL;