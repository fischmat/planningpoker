package de.matthiasfisch.planningpoker.service

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.protocol.JacksonJsonSupport
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.matthiasfisch.planningpoker.model.Event
import dev.failsafe.Failsafe
import dev.failsafe.RetryPolicy
import jakarta.annotation.PreDestroy
import mu.KotlinLogging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.BindException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class EventService(
    @Value("\${websockets.listen-address}") bindAddress: String?,
    @Value("\${websockets.listen-port}") bindPort: Int,
    @Value("\${websockets.allowed-origin}") allowedOrigin: String?
) {
    private val log = logger {}
    private val server: SocketIOServer = createSocketIOServer(bindPort, bindAddress, allowedOrigin)
    private val objectMapper = jacksonObjectMapper()

    fun <T : Event> addListener(eventName: String, eventClass: Class<T>, listener: DataListener<T>) {
        server.addEventListener(
            eventName, String::class.java
        ) { client, data, ackSender ->
            val event = data?.let { objectMapper.readValue(it, eventClass) }
            listener.onData(client, event, ackSender)
        }
    }

    fun broadcastToRoom(room: String, eventName: String, event: Event) {
        server.getRoomOperations(room).sendEvent(eventName, event)
    }

    @PreDestroy
    fun stopSocketIOServer() {
        kotlin.runCatching {
            server.stop()
        }.onFailure {
            log.error(it) { "Failed to close SocketIO server." }
        }.onSuccess {
            log.info { "SocketIO server was successfully stopped." }
        }
    }

    /**
     * Creates a new SocketIO server and starts it.
     * @param bindPort The port to bind to.
     * @param bindHost The hostname to bind to. If null, will bind to 0.0.0.0 or ::0 (any address).
     * @param allowedOrigin If specified, sets the `Access-Control-Allow-Origin` header to this value.
     * @return Returns the started SocketIO server.
     */
    private fun createSocketIOServer(bindPort: Int, bindHost: String?, allowedOrigin: String?): SocketIOServer = try {
        require(bindPort in 1..65535) { "Bind port of SocketIO server must be in range [1, 65535]." }
        val config = Configuration().apply {
            if (!bindHost.isNullOrBlank()) {
                hostname = bindHost
            }
            port = bindPort
            allowedOrigin?.run { origin = allowedOrigin }

            // Use Jackson for deserializing messages, but with support for Java time and Kotlin types
            val kotlinModule = KotlinModule.Builder()
                .configure(KotlinFeature.NullIsSameAsDefault, true)
                .build()
            jsonSupport = JacksonJsonSupport(JavaTimeModule(), kotlinModule)
        }
        SocketIOServer(config).also { server ->
            // Debug log when clients connect or disconnect
            server.addConnectListener { log.debug { "SocketIO client ${it.remoteAddress} has connected." } }
            server.addDisconnectListener { log.debug { "SocketIO client ${it.remoteAddress} disconnected." } }

            // Start the server
            // If started subsequently in short intervals (esp. in dev), the port may still be marked in use
            // Thus, retry the startup in this case for some time...
            val retryPolicy = RetryPolicy.builder<Any>()
                .handle(BindException::class.java)
                .withBackoff(2.seconds.toJavaDuration(), 30.seconds.toJavaDuration())
                .withMaxDuration(2.minutes.toJavaDuration())
                .withMaxRetries(-1)
                .onFailure { log.warn { "SocketIO server could not start: ${it.exception.message}" } }
                .build()
            Failsafe.with(retryPolicy).run { it ->
                log.debug { "Starting SocketIO server (attempt: ${it.attemptCount})" }
                server.start()
                val bindAddress = with(server.configuration) { "${hostname?: "0.0.0.0"}:$port" }
                log.info { "SocketIO server is now available on $bindAddress." }
            }
        }
    } catch (e: Throwable) {
        log.error(e) { "Failed to start SocketIO server." }
        throw e
    }
}