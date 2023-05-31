package de.matthiasfisch.planningpoker.service

import io.minio.errors.ErrorResponseException
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ThemeService(
    private val storageService: StorageService
) {
    fun setCardIcon(gameId: String, iconData: InputStream) {
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