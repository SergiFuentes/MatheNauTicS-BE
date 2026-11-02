package com.mathenautics.backend.domain.repository

import com.mathenautics.backend.dto.GameSaveResponse
import tools.jackson.databind.JsonNode
import java.util.UUID

interface GameSaveRepository {
    fun findAllByUserId(userId: UUID): List<GameSaveResponse>
    fun findById(userId: UUID, saveId: UUID): GameSaveResponse?
    fun create(userId: UUID, gameMode: String, name: String, state: JsonNode): GameSaveResponse
    fun update(userId: UUID, saveId: UUID, name: String, state: JsonNode): GameSaveResponse?
    fun delete(userId: UUID, saveId: UUID): Boolean
}
