package de.matthiasfisch.planningpoker.service

import io.minio.*
import mu.KotlinLogging
import org.apache.commons.compress.utils.IOUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec
import javax.imageio.ImageIO
import kotlin.io.path.*

@Service
class StorageService(
    @Value("\${storage.s3.endpoint}") endpoint: String,
    @Value("\${storage.s3.accessKey}") accessKey: String,
    @Value("\${storage.s3.secretKey}") secretKey: String,
    @Value("\${storage.s3.encryptionKey}") encryptionKey: String?,
    @Value("\${storage.s3.bucket}") private val bucket: String
) {
    private val client by lazy { createS3Client(endpoint, accessKey, secretKey, bucket) }
    private val sse = encryptionKey
        ?.takeIf { it.isNotBlank() }
        ?.let {
            val keyBytes = encryptionKey.toByteArray(StandardCharsets.UTF_8)
            check(keyBytes.size * 8 >= 256) { "At least a 256 bit key is required for SSE, but provided key has only ${keyBytes.size * 8} bits." }
            ServerSideEncryptionCustomerKey(SecretKeySpec(keyBytes, "AES"))
        }
    private val tempDir = createTempDirectory()
    private val log = KotlinLogging.logger {}

    fun storePngImage(objectName: String, data: InputStream) {
        val image = ImageIO.read(data)
        val convertedImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
        convertedImage.graphics.drawImage(image, 0, 0, null, null)
        ByteArrayOutputStream().use {
            ImageIO.write(convertedImage, "png", it)
            storeObject(objectName, ByteArrayInputStream(it.toByteArray()))
        }
    }

    fun storeObject(objectName: String, data: InputStream) {
        val file = createTempFile(directory = tempDir.toAbsolutePath(), prefix = objectName)
        try {
            IOUtils.copy(data, file.outputStream())
            client.uploadObject(
                UploadObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .let {
                        if (sse != null) {
                            it.sse(sse)
                        } else it
                    }
                    .filename(file.absolutePathString())
                    .build()
            )
        } finally {
            file.deleteIfExists()
        }
    }

    fun getObject(objectName: String): InputStream {
        return client.getObject(
            GetObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .let { if (sse != null) it.ssec(sse) else it }
                .build()
        )
    }

    private fun createS3Client(endpoint: String, accessKey: String, secretKey: String, bucket: String): MinioClient {
        val client = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()

        val bucketExists = client.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucket)
                .build()
        )
        if (!bucketExists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            log.info { "Creating S3 bucket '$bucket' as it does not exist yet." }
        }
        return client
    }
}