package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.service.GameService
import de.matthiasfisch.planningpoker.service.PlayerService
import de.matthiasfisch.planningpoker.service.RoundService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/games")
class GameController(
    private val gameService: GameService,
    private val playerService: PlayerService,
    private val roundService: RoundService
) {

    @GetMapping
    fun getAllGames(@RequestParam("page") page: Int?, @RequestParam("limit") pageSize: Int?): PagedResult<Game> {
        return gameService.getPagedGames(
            Pageable.ofSize(pageSize ?: 10)
                .withPage(page ?: 0)
        ).let {
            PagedResult(it)
        }
    }

    @GetMapping("/{gameId}")
    fun getGame(@PathVariable("gameId") gameId: String): Game {
        return gameService.getGame(gameId)
    }

    @GetMapping("/{gameId}/players")
    fun getPlayers(@PathVariable("gameId") gameId: String): List<Player> {
        return gameService.getPlayersInGame(gameId)
    }

    @PostMapping
    fun createGame(@RequestBody stub: GameStub): Game {
        return gameService.createGame(stub)
    }

    @GetMapping("/{gameId}/rounds")
    fun getRounds(@PathVariable("gameId") gameId: String): List<Round> {
        return roundService.getRounds(gameId)
    }

    @GetMapping("/{gameId}/rounds/current")
    fun getCurrentRound(@PathVariable("gameId") gameId: String): Round {
        return roundService.getCurrentRound(gameId)
    }

    @PostMapping("/{gameId}/rounds")
    fun startRound(@PathVariable("gameId") gameId: String, @RequestBody stub: RoundStub): Round {
        return roundService.startRound(gameId, stub)
    }

    @DeleteMapping("/{gameId}/rounds/{roundId}")
    fun endRound(@PathVariable("gameId") gameId: String, @PathVariable("roundId") roundId: String): Round {
        return roundService.endRound(gameId, roundId)
    }

    @PostMapping("/{gameId}/players")
    fun joinGame(@PathVariable("gameId") gameId: String): Player {
        return playerService.joinGame(gameId)
    }

    @DeleteMapping("/{gameId}/players")
    fun leaveGame(@PathVariable("gameId") gameId: String): Player {
        return playerService.leaveGame(gameId)
    }

    @GetMapping("/{gameId}/rounds/{roundId}/votes")
    fun getVotes(@PathVariable("gameId") gameId: String, @PathVariable("roundId") roundId: String): List<Vote> {
        require(roundService.getRound(roundId).gameId == gameId) { "Round $roundId does not belong to game $gameId." }
        return roundService.getVotes(roundId)
    }

    @PostMapping("/{gameId}/rounds/{roundId}/votes")
    fun submitVote(@PathVariable("roundId") roundId: String, @RequestBody card: Card): Vote {
        return roundService.putVote(roundId, card)
    }

    @DeleteMapping("/{gameId}/rounds/{roundId}/votes/mine")
    fun revokeVote(@PathVariable("roundId") roundId: String) {
        roundService.revokeVote(roundId)
    }
}