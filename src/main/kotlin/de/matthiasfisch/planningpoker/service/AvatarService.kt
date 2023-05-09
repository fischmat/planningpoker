package de.matthiasfisch.planningpoker.service

import de.matthiasfisch.planningpoker.model.AvatarProps
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AvatarService(
    @Value("\${avatars.dicebear.uri}") private val baseUrl: String
) {
    private val client = createClient()

    fun getRandomAvatar(seed: String): ByteArray {
        return getAvatar(
            Request(Method.GET, baseUrl).query("seed", seed)
        )
    }

    fun getAvatar(avatar: AvatarProps): ByteArray {
        return getAvatar(
            Request(Method.GET, baseUrl).withAvatarQueryParams(avatar)
        )
    }

    fun getAvatar(request: Request): ByteArray {
        val response = client(request)

        if (!response.status.successful) {
            throw IllegalStateException("Dicebear backend returned status ${response.status}: ${response.bodyString()}")
        }

        return response.body.stream.use {
            it.readAllBytes()
        }
    }

    private fun Request.withAvatarQueryParams(avatar: AvatarProps): Request {
        var request = this

        // background
        request = request.query("backgroundColor", avatar.backgroundColor)

        // eyebrows
        request = request.query("eyebrows", variant(avatar.eyebrows))

        // eyes
        request = request.query("eyes", variant(avatar.eyes))

        // features
        request = request.query("features", avatar.features.joinToString(","))
        request = request.query("featuresProbability", if(avatar.features.isNotEmpty()) "100" else "0")

        // mouth
        request = request.query("mouth", variant(avatar.mouth))

        // skinColor
        request = request.query("skinColor", avatar.skinColor)

        // glasses
        avatar.glasses?.let {
            request = request.query("glasses", variant(it))
            request = request.query("glassesProbability", "100")
        } ?: run {
            request = request.query("glassesProbability", "0")
        }

        // earrings
        avatar.earrings?.let {
            request = request.query("earrings", variant(it))
            request = request.query("earringsProbability", "100")
        } ?: run {
            request = request.query("earringsProbability", "0")
        }

        // hair
        if (avatar.hair != null) {
            val hairVariant = "${if (avatar.longHair) "long" else "short"}${avatar.hair.toString().padStart(2, '0')}"
            request = request.query("hair", hairVariant)
            request = request.query("hairColor", avatar.hairColor)
            request = request.query("hairProbability", "100")
        } else {
            request = request.query("hairProbability", "0")
        }

        return request
    }

    private fun variant(i: Int) =
        "variant${i.toString().padStart(2, '0')}"

    private fun createClient(): HttpHandler {
        val poolingConnectionManager = PoolingHttpClientConnectionManager().apply {
            maxTotal = 10
            defaultMaxPerRoute = 10
        }
        return HttpClientBuilder.create()
            .setConnectionManager(poolingConnectionManager)
            .build()
            .let { ApacheClient(it) }
    }
}