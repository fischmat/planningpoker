package de.matthiasfisch.planningpoker.model

sealed interface Event

data class EnterGameCommand(
    val gameId: String,
    val passwordHash: String?,
    val player: Player
): Event {
    companion object {
        const val EVENT_NAME = "enterGame"
    }
}

data class LeaveGameCommand(
    val gameId: String
): Event {
    companion object {
        const val EVENT_NAME = "leaveGame"
    }
}

data class GameEnteredEvent(
    val gameId: String,
    val roomId: String
): Event {
    companion object {
        const val EVENT_NAME = "gameEntered"
    }
}

data class ErrorEvent(
    val message: String
): Event {
    companion object {
        const val EVENT_NAME = "error"
    }
}

data class PlayerJoinedEvent(
    val gameId: String,
    val player: Player
): Event {
    companion object {
        const val EVENT_NAME = "playerJoined"
    }
}

data class PlayerLeftEvent(
    val gameId: String,
    val player: Player
): Event {
    companion object {
        const val EVENT_NAME = "playerLeft"
    }
}

data class RoundStartedEvent(
    val gameId: String,
    val round: Round
): Event {
    companion object {
        const val EVENT_NAME = "roundStarted"
    }
}

data class RoundEndedEvent(
    val gameId: String,
    val round: Round
): Event {
    companion object {
        const val EVENT_NAME = "roundEnded"
    }
}

data class VoteSubmittedEvent(
    val gameId: String,
    val round: Round,
    val vote: Vote
): Event {
    companion object {
        const val EVENT_NAME = "voteSubmitted"
    }
}

data class VoteRevokedEvent(
    val gameId: String,
    val round: Round,
    val vote: Vote
): Event {
    companion object {
        const val EVENT_NAME = "voteRevoked"
    }
}