package de.matthiasfisch.planningpoker.service

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.minio.*
import io.minio.errors.ErrorResponseException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

@Service
class StorageService(
    @Value("\${storage.s3.endpoint}") endpoint: String,
    @Value("\${storage.s3.accessKey}") accessKey: String,
    @Value("\${storage.s3.secretKey}") secretKey: String,
    @Value("\${storage.s3.encryptionKey}") encryptionKey: String?,
    @Value("\${storage.s3.bucket}") private val bucket: String
) {
    private val log = KotlinLogging.logger {}

    private val client by lazy { createS3Client(endpoint, accessKey, secretKey, bucket) }
    private val sse = encryptionKey
        ?.takeIf { it.isNotBlank() }
        ?.let {
            val keyBytes = encryptionKey.toByteArray(StandardCharsets.UTF_8)
            check(keyBytes.size * 8 >= 256) { "At least a 256 bit key is required for SSE, but provided key has only ${keyBytes.size * 8} bits." }
            ServerSideEncryptionCustomerKey(SecretKeySpec(keyBytes, "AES"))
        }
    private val tempDir = createTempDirectory().also {
        log.info { "Storing temporary files in $it before uploading to S3." }
    }

    fun storePngImage(objectName: String, data: InputStream, maxDimensionPx: Int) {
        val file = createTempFile(directory = tempDir.toAbsolutePath(), prefix = objectName)
        try {
            var image = ImmutableImage.loader()
                .fromStream(data)

            // Scale to maximum dimension
            if (image.width > image.height && image.width > maxDimensionPx) {
                image = image.scaleToWidth(maxDimensionPx)
            } else if (image.height > image.width && image.height > maxDimensionPx) {
                image.scaleToHeight(maxDimensionPx)
            }
            image.output(PngWriter.MaxCompression, file)

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
        } catch (e: Throwable) {
            log.error(e) { "Failed to convert and store PNG image (object name: $objectName)." }
        } finally {
            file.deleteIfExists()
        }
    }

    fun getObject(objectName: String): InputStream? {
        return try {
            client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .let { if (sse != null) it.ssec(sse) else it }
                    .build()
            )
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() == "NoSuchKey") {
                null
            } else {
                throw e
            }
        }
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