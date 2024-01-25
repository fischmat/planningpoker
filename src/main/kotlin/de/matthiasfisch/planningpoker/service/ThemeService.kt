package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.util.ImageUtil
import de.matthiasfisch.planningpoker.util.forbidden
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

@Service
class ThemeService(
    private val storageService: StorageService,
    private val playerService: PlayerService,
    @Value("\${images.card-icon.max-dimension-px}") private val maxDimensionInPx: Int,
    @Value("\${images.card-icon.default-icon-file}") defaultCardIconFilePath: String?
) {
    private val log = KotlinLogging.logger {}

    private val defaultCardIconFile: File? = defaultCardIconFilePath
        ?.takeIf { it.isNotBlank() }
        ?.let { loadDefaultIconFromPath(it) }
    private val tempDir = createTempDirectory().also {
        log.info { "Storing temporary image files in $it before uploading to S3." }
    }

    init {
        require(maxDimensionInPx > 0) { "Maximum icon dimension must be positive." }
    }

    fun setCardIcon(gameId: String, iconData: InputStream) {
        val player = playerService.getPlayer()
        if (!player.gameIds.contains(gameId)) {
            throw forbidden("Player ${player.id} is not in game $gameId.")
        }

        scaleImage(iconData, maxDimensionInPx) {
            try {
                storageService.storeFile(cardIconObjectId(gameId), it.toPath())
            } catch (e: Throwable) {
                throw IOException("Failed to upload card icon for game with ID '$gameId'.", e)
            }
        }
    }

    fun getCardIcon(gameId: String): InputStream? =
        storageService.getObjectOrNull(cardIconObjectId(gameId))?: defaultCardIcon()

    private fun defaultCardIcon(): InputStream =
        defaultCardIconFile?.inputStream() ?: run {
            javaClass.getResourceAsStream("/static/empty.png")
                ?: throw IllegalStateException("Resource /static/empty.png not found!")
        }

    private fun scaleImage(image: InputStream, maxDimensionPx: Int, consumer: (File) -> Unit) {
        val tempScaledImageFile = kotlin.io.path.createTempFile(
            directory = tempDir.toAbsolutePath()
        ).toFile()
        try {
            ImageUtil.scaleImageToDimension(image, tempScaledImageFile, maxDimensionPx)
            consumer(tempScaledImageFile)
        } finally {
            kotlin.runCatching { tempScaledImageFile.delete() }
                .onFailure {
                    log.warn(it) { "Failed to clean up temporary file ${tempScaledImageFile.absolutePath}." }
                }
        }
    }

    private fun cardIconObjectId(gameId: String) = "card-icon-$gameId"

    private fun loadDefaultIconFromPath(path: String) =
        Path.of(path).toFile().also {
            require(it.isFile) { "Default card icon file $path is not a file." }
            require(it.canRead()) { "Default card icon file $path is not readable." }
        }
}