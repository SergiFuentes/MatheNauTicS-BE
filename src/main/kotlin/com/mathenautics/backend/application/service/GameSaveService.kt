package com.mathenautics.backend.application.service

import com.mathenautics.backend.dto.CreateGameSaveRequest
import com.mathenautics.backend.dto.GameSaveResponse
import com.mathenautics.backend.dto.UpdateGameSaveRequest
import java.util.UUID

interface GameSaveService {
    fun list(userId: UUID): List<GameSaveResponse>
    fun get(userId: UUID, saveId: UUID): GameSaveResponse
    fun create(userId: UUID, request: CreateGameSaveRequest): GameSaveResponse
    fun update(userId: UUID, saveId: UUID, request: UpdateGameSaveRequest): GameSaveResponse
    fun delete(userId: UUID, saveId: UUID)
}
