package com.mathenautics.backend.application.service.impl

import com.mathenautics.backend.application.exception.GameSaveNotFoundException
import com.mathenautics.backend.application.service.GameSaveService
import com.mathenautics.backend.domain.repository.GameSaveRepository
import com.mathenautics.backend.domain.repository.UserRepository
import com.mathenautics.backend.dto.CreateGameSaveRequest
import com.mathenautics.backend.dto.GameSaveResponse
import com.mathenautics.backend.dto.UpdateGameSaveRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GameSaveServiceImpl(
    private val gameSaveRepository: GameSaveRepository,
    private val userRepository: UserRepository
) : GameSaveService {

    companion object {
        private const val MAX_NAME_LENGTH = 80
        private val VALID_GAME_MODES = setOf("adventure", "training")
    }

    override fun list(userId: UUID): List<GameSaveResponse> =
        gameSaveRepository.findAllByUserId(userId)

    override fun get(userId: UUID, saveId: UUID): GameSaveResponse =
        gameSaveRepository.findById(userId, saveId) ?: throw GameSaveNotFoundException()

    @Transactional
    override fun create(userId: UUID, request: CreateGameSaveRequest): GameSaveResponse {
        validateName(request.name)
        if (request.gameMode !in VALID_GAME_MODES) {
            throw IllegalArgumentException("Invalid game mode")
        }
        validateState(request.state)
        if (!userRepository.existsById(userId)) {
            throw IllegalArgumentException("User does not exist")
        }

        return gameSaveRepository.create(
            userId = userId,
            gameMode = request.gameMode,
            name = request.name.trim(),
            state = request.state
        )
    }

    @Transactional
    override fun update(
        userId: UUID,
        saveId: UUID,
        request: UpdateGameSaveRequest
    ): GameSaveResponse {
        validateName(request.name)
        validateState(request.state)
        return gameSaveRepository.update(
            userId = userId,
            saveId = saveId,
            name = request.name.trim(),
            state = request.state
        ) ?: throw GameSaveNotFoundException()
    }

    @Transactional
    override fun delete(userId: UUID, saveId: UUID) {
        if (!gameSaveRepository.delete(userId, saveId)) throw GameSaveNotFoundException()
    }

    private fun validateName(name: String) {
        if (name.isBlank() || name.trim().length > MAX_NAME_LENGTH) {
            throw IllegalArgumentException("Save name must contain between 1 and 80 characters")
        }
    }

    private fun validateState(state: tools.jackson.databind.JsonNode) {
        if (!state.isObject) throw IllegalArgumentException("Saved game state must be a JSON object")
    }
}
