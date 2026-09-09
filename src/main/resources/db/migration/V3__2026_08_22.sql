-- ============================================================
-- V3__2026_08_22.sql
-- Update leaderboard to include game mode and exclude guests
-- ============================================================

DROP VIEW IF EXISTS leaderboard;

CREATE VIEW leaderboard AS
SELECT
    u.username,
    gs.game_mode,
    gs.score,
    gs.total_coins,
    gs.created_at
FROM game_sessions gs
         JOIN users u ON gs.user_id = u.id
WHERE u.is_guest = FALSE
ORDER BY gs.score DESC;