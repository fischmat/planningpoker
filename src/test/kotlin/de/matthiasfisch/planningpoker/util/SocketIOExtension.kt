package de.matthiasfisch.planningpoker.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.matthiasfisch.planningpoker.model.*
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeEachListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.socket.client.IO
import io.socket.client.Socket
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val LOGGER = KotlinLogging.logger {}

class SocketIOExtension: AfterSpecListener, BeforeEachListener {
    private val socket: Socket by lazy { connect() }
    val events = mutableListOf<Event>()
    val objectMapper = jacksonObjectMapper()

    fun joinGame(gameId: String, player: Player, passwordHash: String? = null) {
        check(socket.connected()) { "Socket is not connected" }

        val enterFuture = CompletableFuture<Unit>()
        socket.once(GameEnteredEvent.EVENT_NAME) {
            enterFuture.complete(Unit)
        }
        socket.emit(EnterGameCommand.EVENT_NAME, objectMapper.writeValueAsString(EnterGameCommand(gameId, passwordHash, player)))
        enterFuture.get(10, TimeUnit.SECONDS)

        socket.on("") {
            LOGGER.info { it }
        }
    }

    fun leaveGame(gameId: String) {
        socket.emit(LeaveGameCommand.EVENT_NAME, LeaveGameCommand(gameId))
    }

    private fun connect(): Socket {
        val address = "ws://localhost:38081"
        val options = IO.Options().apply {
            transports = arrayOf("websocket")
        }
        val socket = IO.socket(address, options)

        val connectFuture = CompletableFuture<Unit>()
        socket.once(Socket.EVENT_CONNECT) {
            LOGGER.info { "Connected to SocketIO" }
            connectFuture.complete(Unit)
        }
        socket.once(Socket.EVENT_CONNECT_ERROR) {
            connectFuture.completeExceptionally(RuntimeException("Failed to connect to $address."))
        }

        socket.connect()
        connectFuture.get(10, TimeUnit.SECONDS)
        return socket
    }

    override suspend fun afterSpec(spec: Spec) {
        if (socket.connected()) {
            socket.disconnect()
        }
    }

    override suspend fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)
    }

    fun events() = events
}