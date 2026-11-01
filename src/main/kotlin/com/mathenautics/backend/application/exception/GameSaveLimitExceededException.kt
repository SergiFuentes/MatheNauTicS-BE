package com.mathenautics.backend.application.exception

class GameSaveLimitExceededException :
    RuntimeException("A user can have at most 10 saved games")
