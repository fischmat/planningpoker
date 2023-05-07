package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.util.conflict
import de.matthiasfisch.planningpoker.util.notFoundError
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.math.pow
import kotlin.math.sqrt

@Service
@Transactional
class GameService(
    private val playerService: PlayerService,
    private val gameRepo: GameRepository
) {
    fun getPagedGames(pageable: Pageable): Page<Game> {
        return gameRepo.findAll(pageable)
    }

    fun getGame(id: String): Game {
        return gameRepo.findById(id).orElseThrow { notFoundError("Game with ID $id does not exist") }
    }

    fun createGame(stub: GameStub): Game {
        val player = playerService.getPlayer()
        return gameRepo.save(
            with(stub) {
                Game(
                    name = name,
                    password = password,
                    playableCards = playableCards,
                    players = mutableListOf(player),
                    rounds = mutableListOf()
                )
            }
        )
    }

    fun startRound(id: String, stub: RoundStub): Game {
        val game = getGame(id)
        getOngoingRound(game)?.run {
            throw conflict("Round with ID $id is still ongoing.")
        }

        // Add new round
        game.rounds += Round(
            topic = stub.topic
        )
        return gameRepo.save(game)
    }

    fun endRound(gameId: String, roundId: String): Round {
        val game = getGame(gameId)
        val ongoingRound = getOngoingRound(game)
            ?: throw notFoundError("Round with ID $roundId does not exist in game $gameId.")

        game.rounds.remove(ongoingRound)
        game.rounds += ongoingRound.copy(
            ended = Instant.now(),
            statistics = computeRoundResults(ongoingRound)
        )
        return gameRepo.save(game).rounds.first { it.id == ongoingRound.id }
    }

    fun joinGame(id: String): Game {
        val player = playerService.getPlayer()
        val game = getGame(id)
        // If player already in the game, there is nothing to do
        if (isPlayerInGame(player, game)) {
            return game
        }
        game.players += player
        return gameRepo.save(game)
    }

    fun leaveGame(id: String): Game {
        val player = playerService.getPlayer()
        val game = getGame(id)

        if (!isPlayerInGame(player, game)) {
            throw notFoundError("Player ${player.id} did not join game ${game.id}.")
        }

        game.players.remove(player)
        return gameRepo.save(game)
    }

    private fun getOngoingRound(game: Game): Round? {
        val unfinishedRounds = game.rounds.filter { !it.isFinished() }
        check(unfinishedRounds.size <= 1) { "There must be at most one unfinished round in game ${game.id}, but there are ${unfinishedRounds.size}." }
        return unfinishedRounds.firstOrNull()
    }

    private fun isPlayerInGame(player: Player, game: Game): Boolean {
        return game.players.any { it.id == player.id }
    }

    private fun computeRoundResults(round: Round) = with(round) {
        val minVote = votes.minOfOrNull { it.card.value }
        val maxVote = votes.maxOfOrNull { it.card.value }
        val average = votes.map { it.card.value }.average().takeIf { !it.isNaN() }
        val variance = average?.let { avg ->
            sqrt(votes.map { (it.card.value - avg).pow(2.0) }.average())
        }?.takeIf { !it.isNaN() }

        RoundResults(
            votes = votes,
            minVotes = votes.filter { it.card.value == minVote },
            maxVotes = votes.filter { it.card.value == maxVote },
            averageVote = average,
            variance = variance
        )
    }
}