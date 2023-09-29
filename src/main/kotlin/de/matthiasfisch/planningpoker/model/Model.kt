package de.matthiasfisch.planningpoker.model

import java.time.Instant
import java.util.*

interface Creatable {
    val createdAt: Instant
}

interface Updatable {
    val createdAt: Instant
    val updatedAt: Instant
}

data class Avatar(
    val backgroundColor: String,
    val earrings: Int?,
    val eyebrows: Int,
    val eyes: Int,
    val features: List<String>,
    val glasses: Int?,
    val hair: Int?,
    val longHair: Boolean,
    val hairColor: String,
    val mouth: Int,
    val skinColor: String
)

data class Player(
    val id: UUID,
    val name: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val avatar: Avatar?
): Updatable

data class Card(
    val value: Int
)

data class Vote(
    val playerId: UUID,
    val roundId: UUID,
    override val createdAt: Instant,
    val card: Card
): Creatable

data class RoundStatistics(
    val votes: List<Vote>,
    val minVoteValue: Int?,
    val maxVoteValue: Int?,
    val minVotes: List<Vote>,
    val maxVotes: List<Vote>,
    val suggestedCard: Card?,
    val averageVote: Double?,
    val variance: Double?
)

data class Round(
    val id: UUID,
    val gameId: UUID,
    val topic: String?,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val endedAt: Instant?,
    val endedByPlayerId: UUID?
): Updatable

data class Game(
    val id: UUID,
    val name: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val hasPassword: Boolean,
    val playableCards: List<Card>
): Updatable