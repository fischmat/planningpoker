package de.matthiasfisch.planningpoker.service

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.DataListener
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.matthiasfisch.planningpoker.model.Event
import jakarta.annotation.PreDestroy
import mu.KotlinLogging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class EventService(
    @Value("\${websockets.listen-address}") bindAddress: String,
    @Value("\${websockets.port}") val bindPort: Int,
    @Value("\${websockets.allowed-origin}") val allowedOrigin: String?,
    private val applicationContext: ApplicationContext
) {
    private val log = logger {}
    private val server: SocketIOServer
    private val objectMapper = jacksonObjectMapper()

    init {
        val config = Configuration().apply {
            if (bindAddress.isNotBlank()) {
                hostname = bindAddress
            }
            port = bindPort
            allowedOrigin?.let { origin = it }
        }
        server = SocketIOServer(config)
        server.addConnectListener { log.debug { "SocketIO client ${it.remoteAddress} has connected." } }
        server.addDisconnectListener { log.debug { "SocketIO client ${it.remoteAddress} disconnected." } }

        log.info { "Starting SocketIO server on ${bindAddress.takeIf { it.isNotBlank() } ?: "0.0.0.0"}:$bindPort." }
        kotlin.runCatching {
            server.start()
        }.onFailure { e ->
            SpringApplication.exit(applicationContext, object: ExitCodeGenerator {
                override fun getExitCode(): Int {
                    log.error(e) { "Could not start Socket.IO Server. Terminating the application..." }
                    return 1
                }
            })
        }
    }

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
            log.info { "Closed SocketIO server." }
        }
    }
}