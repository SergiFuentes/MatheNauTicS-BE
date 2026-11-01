package com.mathenautics.backend.application.controller

import com.mathenautics.backend.application.service.GameSaveService
import com.mathenautics.backend.application.service.GameSessionService
import com.mathenautics.backend.dto.*
import com.mathenautics.backend.security.AuthenticatedUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/games")
class GameController(
    private val gameService: GameSessionService,
    private val gameSaveService: GameSaveService
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

    @GetMapping("/saves")
    fun listSaves(authentication: Authentication): ResponseEntity<List<GameSaveResponse>> {
        val user = authentication.principal as AuthenticatedUser
        return ResponseEntity.ok(gameSaveService.list(user.userId))
    }

    @PostMapping("/saves")
    fun createSave(
        @RequestBody request: CreateGameSaveRequest,
        authentication: Authentication
    ): ResponseEntity<GameSaveResponse> {
        val user = authentication.principal as AuthenticatedUser
        return ResponseEntity.status(HttpStatus.CREATED).body(gameSaveService.create(user.userId, request))
    }

    @GetMapping("/saves/{saveId}")
    fun getSave(
        @PathVariable saveId: UUID,
        authentication: Authentication
    ): ResponseEntity<GameSaveResponse> {
        val user = authentication.principal as AuthenticatedUser
        return ResponseEntity.ok(gameSaveService.get(user.userId, saveId))
    }

    @PutMapping("/saves/{saveId}")
    fun updateSave(
        @PathVariable saveId: UUID,
        @RequestBody request: UpdateGameSaveRequest,
        authentication: Authentication
    ): ResponseEntity<GameSaveResponse> {
        val user = authentication.principal as AuthenticatedUser
        return ResponseEntity.ok(gameSaveService.update(user.userId, saveId, request))
    }

    @DeleteMapping("/saves/{saveId}")
    fun deleteSave(
        @PathVariable saveId: UUID,
        authentication: Authentication
    ): ResponseEntity<Unit> {
        val user = authentication.principal as AuthenticatedUser
        gameSaveService.delete(user.userId, saveId)
        return ResponseEntity.noContent().build()
    }
}
