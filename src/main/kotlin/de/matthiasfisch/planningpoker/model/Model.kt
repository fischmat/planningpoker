package de.matthiasfisch.planningpoker.model

import de.matthiasfisch.planningpoker.util.JsonMasked
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import java.time.Instant

@Document("players")
data class Player (
    @Id val id: String? = null,
    val name: String
)

@Document("games")
data class Game(
    @Id val id: String? = null,
    val name: String,
    @JsonMasked val password: String?,
    val playableCards: List<Card>,

    val rounds: MutableList<Round>,
    val players: MutableList<Player>
)

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

interface GameRepository: MongoRepository<Game, String> {
    fun findByName(name: String): Game?
}

interface PlayerRepository: MongoRepository<Player, String>