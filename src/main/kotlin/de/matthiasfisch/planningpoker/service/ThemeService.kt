package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.util.forbidden
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Path

@Service
class ThemeService(
    private val storageService: StorageService,
    private val playerService: PlayerService,
    @Value("\${images.card-icon.default-icon-file}") defaultCardIconFilePath: String?
) {
    private val defaultCardIconFile: File? = defaultCardIconFilePath
        ?.takeIf { it.isNotBlank() }
        ?.let { loadDefaultIconFromPath(it) }

    fun setCardIcon(gameId: String, iconData: InputStream) {
        val player = playerService.getPlayer()
        if (!player.gameIds.contains(gameId)) {
            throw forbidden("Player ${player.id} is not in game $gameId.")
        }
        storageService.storePngImage(cardIconObjectId(gameId), iconData, 80)
    }

    fun getCardIcon(gameId: String): InputStream? =
        storageService.getObject(cardIconObjectId(gameId))?: defaultCardIcon()

    private fun defaultCardIcon(): InputStream =
        defaultCardIconFile?.inputStream() ?: run {
            javaClass.getResourceAsStream("/static/empty.png")
                ?: throw IllegalStateException("Resource /static/empty.png not found!")
        }

    private fun cardIconObjectId(gameId: String) = "card-icon-$gameId"

    private fun loadDefaultIconFromPath(path: String) =
        Path.of(path).toFile().also {
            require(it.isFile) { "Default card icon file $path is not a file." }
            require(it.canRead()) { "Default card icon file $path is not readable." }
        }
}