package de.matthiasfisch.planningpoker.util

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import java.io.File
import java.io.IOException
import java.io.InputStream

object ImageUtil {

    fun scaleImageToDimension(inputStream: InputStream, targetFile: File, targetMaxSizeInPx: Int) {
        require(targetFile.exists() || targetFile.createNewFile()) {
            "Target file $targetFile for scaled image does not exist and can't be created."
        }
        require(targetFile.canWrite()) {
            "Target file $targetFile for scaled image is not writable."
        }

        try {
            val image = ImmutableImage.loader()
                .fromStream(inputStream)

            // Scale the bigger dimension to the target dimension (if not already smaller)
            val scaled = if (image.width > image.height && image.width > targetMaxSizeInPx) {
                image.scaleToWidth(targetMaxSizeInPx)
            } else if (image.height > image.width && image.height > targetMaxSizeInPx) {
                image.scaleToHeight(targetMaxSizeInPx)
            } else {
                // If image already smaller, keep it as it is
                image
            }

            scaled.output(PngWriter.MaxCompression, targetFile)
        } catch (e: IOException) {
            throw IOException("Could not scale image (target file: $targetFile) due to an I/O error.", e)
        }
    }
}