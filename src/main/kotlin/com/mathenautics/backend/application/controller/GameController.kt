package com.mathenautics.backend.application.controller

import com.mathenautics.backend.application.service.GameSessionService
import com.mathenautics.backend.dto.*
import com.mathenautics.backend.security.AuthenticatedUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/games")
class GameController(
    private val gameService: GameSessionService
) {

    @PostMapping("/finish")
    fun finishGame(
        @RequestBody request: GameResultRequest,
        authentication: Authentication
    ): ResponseEntity<GameResultResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val response = gameService.finishGame(authenticatedUser.userId, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/player/coins")
    fun getPlayerCoins(authentication: Authentication): ResponseEntity<PlayerCoinsResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val response = gameService.getCurrentCoins(authenticatedUser.userId)
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
    fun getProgress(
        @RequestParam gameMode: String,
        authentication: Authentication
    ): ResponseEntity<PlayerProgressResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val progress = gameService.getPlayerProgress(authenticatedUser.userId, gameMode)
        return ResponseEntity.ok(progress)
    }

    @PostMapping("/progress")
    fun updateProgress(
        @RequestBody request: PlayerProgressRequest,
        authentication: Authentication
    ): ResponseEntity<PlayerProgressResponse> {
        val authenticatedUser = authentication.principal as AuthenticatedUser
        val updated = gameService.updatePlayerProgress(authenticatedUser.userId, request)
        return ResponseEntity.ok(updated)
    }
}