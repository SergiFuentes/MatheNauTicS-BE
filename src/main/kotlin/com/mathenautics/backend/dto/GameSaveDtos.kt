package com.mathenautics.backend.dto

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class CreateGameSaveRequest(
    val gameMode: String,
    val name: String,
    val state: JsonNode
)

data class UpdateGameSaveRequest(
    val name: String,
    val state: JsonNode
)

data class GameSaveResponse(
    val id: UUID,
    val userId: UUID,
    val gameMode: String,
    val name: String,
    val state: JsonNode,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
