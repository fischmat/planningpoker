package de.matthiasfisch.planningpoker.model

import de.matthiasfisch.planningpoker.util.JsonMasked
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

@Document("players")
data class Player (
    @Id val id: String? = null,
    val name: String,
    val avatar: AvatarProps?
)

@Document("games")
data class Game(
    @Id val id: String? = null,
    val name: String,
    @JsonMasked val password: String?,
    val playableCards: List<Card>,

    val rounds: MutableList<Round> = mutableListOf(),
    val players: MutableList<Player> = mutableListOf()
) {
    init {
        require(name.isNotEmpty()) { "Name of game must not be blank." }
        require(password == null || password.isNotEmpty()) { "Password must be not blank if specified." }
        require(playableCards.isNotEmpty()) { "No playable cards are set for the game." }
        val duplicateCards = playableCards.filter { c ->
            playableCards.count { it.value == c.value } > 1
        }.distinct().sortedBy { it.value }
        require(duplicateCards.isEmpty()) {
            "Playable cards are duplicated: ${duplicateCards.map { it.value }.joinToString(", ")}"
        }
    }
}

data class Card(
    val value: Int
)

data class Round(
    @Id val id: String? = null,
    val topic: String,
    val started: Instant = Instant.now(),
    val ended: Instant? = null,
    val votes: MutableList<Vote> = mutableListOf(),
    val statistics: RoundResults? = null
) {
    fun isFinished() = ended != null
}

data class RoundResults(
    val votes: List<Vote>,
    val minVotes: List<Vote>,
    val maxVotes: List<Vote>,
    val averageVote: Double?,
    val variance: Double?
)

data class Vote(
    val player: Player,
    val card: Card
)

// Repositories

interface GameRepository: MongoRepository<Game, String>

interface PlayerRepository: MongoRepository<Player, String>