package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.service.GameService
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/games")
class GameController(
    private val gameService: GameService
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

    @PostMapping
    fun createGame(@RequestBody stub: GameStub): Game {
        return gameService.createGame(stub)
    }

    @PostMapping("/{gameId}/rounds")
    fun startRound(@PathVariable("gameId") gameId: String, @RequestBody stub: RoundStub): List<Round> {
        return gameService.startRound(gameId, stub).rounds
    }

    @DeleteMapping("/{gameId}/rounds/{roundId}")
    fun endRound(@PathVariable("gameId") gameId: String, @PathVariable("roundId") roundId: String): Round {
        return gameService.endRound(gameId, roundId)
    }

    @PostMapping("/{gameId}/players")
    fun joinGame(@PathVariable("gameId") gameId: String): List<Player> {
        return gameService.joinGame(gameId).players
    }

    @DeleteMapping("/{gameId}/players")
    fun leaveGame(@PathVariable("gameId") gameId: String): List<Player> {
        return gameService.leaveGame(gameId).players
    }
}