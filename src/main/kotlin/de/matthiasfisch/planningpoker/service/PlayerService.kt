package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.PlayerRepository
import de.matthiasfisch.planningpoker.model.PlayerStub
import de.matthiasfisch.planningpoker.util.badRequestError
import de.matthiasfisch.planningpoker.util.notFoundError
import de.matthiasfisch.planningpoker.util.unauthorized
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service

private const val SESSION_PLAYER_ID_ATTR = "playerId"

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
    private val session: HttpSession,
    private val gameEvents: GameEventService
) {
    fun getPlayer(): Player {
        return currentPlayerId()?.let { getPlayer(it) } ?: throw unauthorized()
    }

    fun getPlayer(id: String): Player =
        playerRepository.findById(id)
            .orElseThrow { notFoundError("Player with ID $id does not exist") }

    fun isPlayerSession() = currentPlayerId() != null

    fun updatePlayer(stub: PlayerStub): Player {
        val player = getPlayer()
        return playerRepository.save(
            player.copy(
                name = stub.name,
                avatar = stub.avatar
            )
        )
    }

    fun getOrUpdatePlayer(stub: PlayerStub): Player {
        val playerId = currentPlayerId()

        return if (playerId != null) {
            updatePlayer(stub)
        } else {
            playerRepository.save(
                Player(
                    name = stub.name,
                    gameIds = mutableListOf(),
                    avatar = stub.avatar
                )
            ).also {
                session.setAttribute(SESSION_PLAYER_ID_ATTR, it.id)
            }
        }
    }

    fun joinGame(gameId: String): Player {
        val player = getPlayer()
        if (player.gameIds.any { it == gameId }) {
            return player
        }
        player.gameIds.add(gameId)
        return playerRepository.save(player).also {
            gameEvents.notifyPlayerJoined(gameId, it)
        }
    }

    fun leaveGame(gameId: String): Player {
        val player = getPlayer()
        if (player.gameIds.none { it == gameId }) {
            throw badRequestError("Player ${player.id} did not join game ${gameId}.")
        }
        player.gameIds.remove(gameId)
        return playerRepository.save(player).also {
            gameEvents.notifyPlayerLeft(gameId, it)
        }
    }

    fun getPlayersInGame(gameId: String): List<Player> {
        return playerRepository.findByGameId(gameId)
    }

    private fun currentPlayerId() = session.getAttribute(SESSION_PLAYER_ID_ATTR).let { it as? String }
}