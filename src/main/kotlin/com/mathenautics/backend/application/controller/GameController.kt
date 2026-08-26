package com.mathenautics.backend.application.controller

import com.mathenautics.backend.application.service.GameSessionService
import com.mathenautics.backend.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/games")
class GameController(
    private val gameService: GameSessionService
) {

    @PostMapping("/finish")
    fun finishGame(@RequestBody request: GameResultRequest): ResponseEntity<GameResultResponse> {
        val response = gameService.finishGame(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/player/coins")
    fun getPlayerCoins(@RequestParam userId: UUID): ResponseEntity<PlayerCoinsResponse> {
        val response = gameService.getCurrentCoins(userId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/leaderboard")
    fun getLeaderboard(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(required = false) gameMode: String?
    ): ResponseEntity<List<LeaderboardEntry>> {
        val leaderboard = gameService.getLeaderboard(limit, offset, gameMode)
        return ResponseEntity.ok(leaderboard)
    }

    @GetMapping("/progress")
    fun getProgress(@RequestParam userId: UUID): ResponseEntity<PlayerProgressResponse> {
        val progress = gameService.getPlayerProgress(userId)
        return ResponseEntity.ok(progress)
    }

    @PostMapping("/progress")
    fun updateProgress(@RequestBody request: PlayerProgressRequest): ResponseEntity<PlayerProgressResponse> {
        val updated = gameService.updatePlayerProgress(request)
        return ResponseEntity.ok(updated)
    }
}