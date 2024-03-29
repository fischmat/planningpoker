package de.matthiasfisch.planningpoker.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import java.time.Instant

@Document("players")
data class Player (
    @Id val id: String? = null,
    val name: String,
    val gameIds: MutableList<String>,
    val avatar: AvatarProps? = null
)

@Document("games")
@JsonIgnoreProperties(ignoreUnknown = true)
data class Game(
    @Id val id: String? = null,
    val name: String,
    @JsonIgnore val passwordHash: String?,
    val playableCards: List<Card>
) {
    init {
        require(name.isNotEmpty()) { "Name of game must not be blank." }
        require(passwordHash == null || passwordHash.isNotEmpty()) { "Password must be not blank if specified." }
        require(playableCards.isNotEmpty()) { "No playable cards are set for the game." }
        val duplicateCards = playableCards.filter { c ->
            playableCards.count { it.value == c.value } > 1
        }.distinct().sortedBy { it.value }
        require(duplicateCards.isEmpty()) {
            "Playable cards are duplicated: ${duplicateCards.map { it.value }.joinToString(", ")}"
        }
    }

    @JsonProperty("hasPassword") fun hasPassword() = passwordHash != null
}

data class Card(
    val value: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Round(
    @Id val id: String? = null,
    val gameId: String,
    val topic: String,
    val started: Instant = Instant.now(),
    val ended: Instant? = null,
    val result: RoundResults? = null
) {
    fun isFinished() = ended != null
}

data class RoundResults(
    val votes: List<Vote>,
    val minVoteValue: Int?,
    val maxVoteValue: Int?,
    val minVotes: List<Vote>,
    val maxVotes: List<Vote>,
    val suggestedCardConservative: Card?,
    val suggestedCardMajority: Card?,
    val averageVote: Double?,
    val variance: Double?
)

data class Vote(
    @Id val id: String? = null,
    val gameId: String,
    val roundId: String,
    val player: Player,
    val card: Card
)

// Repositories

interface GameRepository: MongoRepository<Game, String>

interface PlayerRepository: MongoRepository<Player, String> {
    @Query(value = "{ 'gameIds': { \$all: [?0] } }")
    fun findByGameId(gameId: String): List<Player>
}

interface RoundRepository: MongoRepository<Round, String> {
    fun findByGameId(gameId: String): List<Round>
    fun findByGameIdAndEnded(gameId: String, ended: Instant?): List<Round>
}

interface VoteRepository: MongoRepository<Vote, String> {
    fun findByRoundId(roundId: String): List<Vote>
}