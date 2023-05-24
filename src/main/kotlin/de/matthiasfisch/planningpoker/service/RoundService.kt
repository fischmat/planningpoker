package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.util.badRequestError
import de.matthiasfisch.planningpoker.util.conflict
import de.matthiasfisch.planningpoker.util.forbidden
import de.matthiasfisch.planningpoker.util.notFoundError
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.pow
import kotlin.math.sqrt

@Service
class RoundService(
    private val roundRepository: RoundRepository,
    private val gameService: GameService,
    private val playerService: PlayerService,
    private val voteRepository: VoteRepository,
    private val gameEvents: GameEventService
) {
    fun getRound(roundId: String): Round {
        return roundRepository.findById(roundId).orElseThrow {
            notFoundError("Round with ID '$roundId' does not exist.")
        }
    }

    fun getRounds(gameId: String): List<Round> {
        gameService.getGame(gameId) // Check game exists
        checkPlayerIsInGame(gameId)
        return roundRepository.findByGameId(gameId)
    }

    fun getCurrentRound(gameId: String): Round {
        return getRounds(gameId)
            .singleOrNull { !it.isFinished() }
            ?: throw notFoundError("No round is currently ongoing.")
    }

    fun startRound(gameId: String, stub: RoundStub): Round {
        gameService.getGame(gameId) // Check game exists
        val ongoingRound = getOngoingRound(gameId)
        if (ongoingRound != null) {
            throw conflict("Round with ID ${ongoingRound.id} is still ongoing.")
        }
        checkPlayerIsInGame(gameId)

        return roundRepository.save(
            Round(
                gameId = gameId,
                topic = stub.topic
            )
        ).also {
            gameEvents.notifyPlayerRoundStarted(gameId, it)
        }
    }

    fun endRound(gameId: String, roundId: String): Round {
        gameService.getGame(gameId) // Check game exists
        val round = roundRepository.findById(roundId).orElseThrow {
            throw notFoundError("Round with ID $roundId does not exist")
        }
        checkPlayerIsInGame(gameId)
        if (round.isFinished()) {
            throw badRequestError("Round with ID '$roundId' is already finished.")
        }

        val finishedRound = round.copy(
            ended = Instant.now(),
            result = computeRoundResults(round)
        )
        return roundRepository.save(finishedRound).also {
            gameEvents.notifyPlayerRoundEnded(gameId, it)
        }
    }

    fun getVotes(roundId: String): List<Vote> {
        val gameId = getRound(roundId).gameId
        checkPlayerIsInGame(gameId)
        return voteRepository.findByRoundId(roundId)
    }

    fun putVote(roundId: String, card: Card): Vote {
        val round = getRound(roundId)
        val game = gameService.getGame(round.gameId)
        checkPlayerIsInGame(round.gameId)

        if (card !in game.playableCards) {
            throw badRequestError("Card with value ${card.value} can not be played in game ${game.id}.")
        }

        revokeVote(roundId)

        val player = playerService.getPlayer()
        return voteRepository.save(
            Vote(
                gameId = round.gameId,
                roundId = round.id!!,
                player = player,
                card = card
            )
        ).also {
            gameEvents.notifyPlayerVoteSubmitted(round.gameId, round, it)
        }
    }

    fun revokeVote(roundId: String) {
        val round = getRound(roundId)
        checkPlayerIsInGame(round.gameId)

        val player = playerService.getPlayer()
        voteRepository.findByRoundId(roundId)
            .filter { it.player.id == player.id }
            .forEach { vote ->
                voteRepository.deleteById(vote.id!!).also {
                    gameEvents.notifyPlayerVoteRevoked(round.gameId, round, vote)
                }
            }
    }

    fun getRoundResults(roundId: String): RoundResults {
        val round = getRound(roundId)
        checkPlayerIsInGame(round.gameId)
        if (!round.isFinished()) {
            throw conflict("Round $roundId is not ended yet.")
        }
        return computeRoundResults(round)
    }

    private fun computeRoundResults(round: Round): RoundResults {
        val votes = getVotes(round.id!!)
        val minVote = votes.minOfOrNull { it.card.value }
        val maxVote = votes.maxOfOrNull { it.card.value }
        val average = votes.map { it.card.value }.average().takeIf { !it.isNaN() }
        val variance = average?.let { avg ->
            sqrt(votes.map { (it.card.value - avg).pow(2.0) }.average())
        }?.takeIf { !it.isNaN() }
        val suggestedCard = if (average != null) {
            gameService.getGame(round.gameId)
                .playableCards
                .filter { it.value >= average }
                .minByOrNull { it.value }
        } else {
            null
        }

        return RoundResults(
            votes = votes,
            minVoteValue = minVote,
            maxVoteValue = maxVote,
            minVotes = votes.filter { it.card.value == minVote },
            maxVotes = votes.filter { it.card.value == maxVote },
            averageVote = average,
            variance = variance,
            suggestedCard = suggestedCard
        )
    }

    private fun checkPlayerIsInGame(gameId: String) {
        val player = playerService.getPlayer()
        if(player.gameIds.none { it == gameId }) {
            throw forbidden("Player with ID '${player.id}' is not part of game '${gameId}'")
        }
    }

    private fun getOngoingRound(gameId: String) =
        roundRepository.findByGameIdAndEnded(gameId = gameId, ended = null).firstOrNull()
}