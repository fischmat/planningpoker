package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.PlayerRepository
import de.matthiasfisch.planningpoker.util.notFoundError
import de.matthiasfisch.planningpoker.util.unauthorized
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Service

private const val SESSION_PLAYER_ID_ATTR = "playerId"

@Service
class PlayerService(
    private val playerRepo: PlayerRepository,
    private val session: HttpSession
) {
    fun getPlayer(): Player {
        return currentPlayerId()?.let { getPlayer(it) } ?: throw unauthorized()
    }

    fun getPlayer(id: String): Player =
        playerRepo.findById(id)
            .orElseThrow { notFoundError("Player with ID $id does not exist") }

    fun getOrCreatePlayer(name: String): Player {
        val playerId = currentPlayerId()

        return if (playerId != null) {
            playerRepo.findById(playerId)
                .orElseThrow { notFoundError("Player $playerId does not exist") }
        } else {
            playerRepo.save(
                Player(
                    name = name
                )
            ).also {
                session.setAttribute(SESSION_PLAYER_ID_ATTR, it.id)
            }
        }
    }

    private fun currentPlayerId() = session.getAttribute(SESSION_PLAYER_ID_ATTR).let { it as? String }
}