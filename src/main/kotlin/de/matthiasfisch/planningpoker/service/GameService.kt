package de.matthiasfisch.planningpoker.service

import com.corundumstudio.socketio.SocketIOClient
import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.util.forbidden
import de.matthiasfisch.planningpoker.util.notFoundError
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
@Transactional
class GameService(
    private val playerService: PlayerService,
    private val gameRepo: GameRepository,
    private val passwordHashing: PasswordHashingService
) {
    fun getPagedGames(pageable: Pageable): Page<Game> {
        return gameRepo.findAll(pageable)
    }

    fun getGame(id: String): Game {
        return gameRepo.findById(id).orElseThrow { notFoundError("Game with ID $id does not exist") }
    }

    fun createGame(stub: GameStub): Game {
        return gameRepo.save(
            with(stub) {
                Game(
                    name = name,
                    passwordHash = password?.let { passwordHashing.encodePlaintext(it) },
                    playableCards = playableCards
                )
            }
        ).also {
            if (playerService.isPlayerSession()) {
                playerService.joinGame(it.id!!)
            }
        }
    }

    fun getPlayersInGame(gameId: String): List<Player> {
        val game = getGame(gameId)
        val player = playerService.getPlayer()
        if (player.gameIds.none { it == game.id }) {
            throw forbidden("Player did not join game $gameId")
        }
        return playerService.getPlayersInGame(gameId)
    }
}

@Service
class GameEventService(
    private val eventService: EventService,
    private val gameRepository: GameRepository,
    private val passwordHashingService: PasswordHashingService
) {
    private val roomPrefix = "games/"

    init {
        eventService.addListener(EnterGameCommand.EVENT_NAME, EnterGameCommand::class.java) { client, event, _ ->
            handleGameJoined(client, event)
        }

        eventService.addListener(LeaveGameCommand.EVENT_NAME, LeaveGameCommand::class.java) { client, event, _ ->
            client.leaveRoom(gameRoomId(event.gameId))
        }
    }

    fun notifyPlayerJoined(gameId: String, player: Player) =
        broadcast(gameId, PlayerJoinedEvent.EVENT_NAME, PlayerJoinedEvent(gameId, player))

    fun notifyPlayerLeft(gameId: String, player: Player) =
        broadcast(gameId, PlayerLeftEvent.EVENT_NAME, PlayerLeftEvent(gameId, player))

    fun notifyPlayerRoundStarted(gameId: String, round: Round) =
        broadcast(gameId, RoundStartedEvent.EVENT_NAME, RoundStartedEvent(gameId, round))

    fun notifyPlayerRoundEnded(gameId: String, round: Round) =
        broadcast(gameId, RoundEndedEvent.EVENT_NAME, RoundEndedEvent(gameId, round))

    fun notifyPlayerVoteSubmitted(gameId: String, round: Round, vote: Vote) =
        broadcast(gameId, VoteSubmittedEvent.EVENT_NAME, VoteSubmittedEvent(gameId, round, vote))

    fun notifyPlayerVoteRevoked(gameId: String, round: Round, vote: Vote) =
        broadcast(gameId, VoteRevokedEvent.EVENT_NAME, VoteRevokedEvent(gameId, round, vote))

    private fun handleGameJoined(client: SocketIOClient, event: EnterGameCommand) {
        val game = gameRepository.findById(event.gameId)
        if (game.isEmpty) {
            LOGGER.debug { "Client (${client.remoteAddress}) tried to join non-existing game '${event.gameId}'." }
            client.sendEvent(ErrorEvent.EVENT_NAME, ErrorEvent("Game with ID '${event.gameId}' does not exist."))
            return
        }

        val expectedPasswordHash = game.get().passwordHash
        if (expectedPasswordHash != null
            && (event.passwordHash == null || !passwordHashingService.intermediateMatches(event.passwordHash, expectedPasswordHash))
        ) {
            LOGGER.info { "Rejected client (${client.remoteAddress}) from joining game '${event.gameId}': Wrong password" }
            client.sendEvent(ErrorEvent.EVENT_NAME, ErrorEvent("Provided password is wrong."))
            return
        }

        val room = gameRoomId(event.gameId)
        client.joinRoom(room)
        client.sendEvent(GameEnteredEvent.EVENT_NAME, GameEnteredEvent(event.gameId, room))
        LOGGER.debug { "Client (${client.remoteAddress}) subscribed for events from game '${event.gameId}'." }
    }

    private fun broadcast(gameId: String, eventName: String, event: Event) {
        val room = gameRoomId(gameId)
        LOGGER.debug { "Broadcasting event '$eventName' to room $room: $event" }
        eventService.broadcastToRoom(room, eventName, event)
    }

    private fun gameRoomId(gameId: String) = roomPrefix + gameId
}