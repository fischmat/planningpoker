package de.matthiasfisch.planningpoker.model

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL

@Component
data class ApplicationInfo(
    val socketIO: SocketIOInfo
)

@Component
class SocketIOInfo(
    @Value("\${server.url}") host: String,
    @Value("\${websockets.published-port}") val port: Int,
    @Value("\${websockets.protocol}")  val scheme: String
) {
    val host: String = URL(host).host
}