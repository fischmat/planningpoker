package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.util.forbidden
import io.minio.errors.ErrorResponseException
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ThemeService(
    private val storageService: StorageService,
    private val playerService: PlayerService
) {
    fun setCardIcon(gameId: String, iconData: InputStream) {
        val player = playerService.getPlayer()
        if (!player.gameIds.contains(gameId)) {
            throw forbidden("Player ${player.id} is not in game $gameId.")
        }
        storageService.storePngImage(cardIconObjectId(gameId), iconData, 256)
    }

    fun getCardIcon(gameId: String): InputStream? {
        return try {
            storageService.getObject(cardIconObjectId(gameId))
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                defaultCardIcon()
            } else {
                throw e
            }
        }
    }

    private fun defaultCardIcon(): InputStream {
        // TODO Make default image configurable
        return javaClass.getResourceAsStream("/static/empty.png")
            ?: throw IllegalStateException("Resource /static/empty.png not found!")
    }

    private fun cardIconObjectId(gameId: String) = "card-icon-$gameId"
}