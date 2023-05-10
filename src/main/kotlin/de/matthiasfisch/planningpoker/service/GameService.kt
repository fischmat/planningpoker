package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.model.Game
import de.matthiasfisch.planningpoker.model.GameRepository
import de.matthiasfisch.planningpoker.model.GameStub
import de.matthiasfisch.planningpoker.util.notFoundError
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GameService(
    private val playerService: PlayerService,
    private val gameRepo: GameRepository,
    private val passwordHashing: PasswordHashingService
) {
    fun getPagedGames(pageable: Pageable): Page<Game> {
        return gameRepo.findAll(pageable)
    }

    fun getGame(id: String): Game {
        return gameRepo.findById(id).orElseThrow { notFoundError("Game with ID $id does not exist") }
    }

    fun createGame(stub: GameStub): Game {
        playerService.getPlayer() // Check player exists
        return gameRepo.save(
            with(stub) {
                Game(
                    name = name,
                    passwordHash = password?.let { passwordHashing.encodePlaintext(it) },
                    playableCards = playableCards
                )
            }
        ).also {
            playerService.joinGame(it.id!!)
        }
    }
}