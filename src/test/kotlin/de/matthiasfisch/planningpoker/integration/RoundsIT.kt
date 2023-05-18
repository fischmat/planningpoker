package de.matthiasfisch.planningpoker.integration

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.controller.Players
import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.service.PasswordHashingService
import de.matthiasfisch.planningpoker.util.CleanupExtension
import de.matthiasfisch.planningpoker.util.RestAssuredExtension
import de.matthiasfisch.planningpoker.util.SocketIOExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class RoundsIT(
    @LocalServerPort val serverPort: Int
): FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, RestAssuredExtension(serverPort))
    val cleanup = extension(CleanupExtension())
    val socketIO = extension(SocketIOExtension())

    @Autowired
    private lateinit var gameRepository: GameRepository

    @Autowired
    private lateinit var playerRepository: PlayerRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    @Autowired
    private lateinit var passwordHashing: PasswordHashingService

    fun createGame(name: String, password: String?, cards: List<Int>, players: List<Player> = listOf()): Game {
        return gameRepository.save(Game(
            name = name,
            passwordHash = password?.let { passwordHashing.encodePlaintext(it) },
            playableCards = cards.map { Card(it) }
        )).also { game ->
            players.forEach { player ->
                player.gameIds.add(game.id!!)
                playerRepository.save(player)
            }
        }.also {
            cleanup.addTask { gameRepository.delete(it) }
        }
    }

    init {
        context("GET /v1/games/{gameId}/rounds - Get all rounds") {
            val (player, sessionId) = Players.createPlayerSession()
            val game = createGame(
                name = "some-game",
                password = null,
                cards = listOf(1, 2, 3),
                players = listOf(player)
            )
            val firstRoundId = Api.games.startRound(game.id!!, RoundStub("round 1"), sessionId).id!!
            Api.games.endRound(game.id!!, firstRoundId, sessionId)
            val secondRoundId = Api.games.startRound(game.id!!, RoundStub("round 2"), sessionId).id!!

            test("Get rounds of existing game -> 200") {
                // Arrange

                // Act
                val rounds = Api.games.getRounds(game.id!!, sessionId)

                // Assert
                rounds.shouldHaveSize(2)

                rounds[0]["id"] shouldBe firstRoundId
                rounds[0]["topic"] shouldBe "round 1"
                rounds[0]["started"].shouldNotBeNull()
                rounds[0]["ended"].shouldNotBeNull()

                rounds[1]["id"] shouldBe secondRoundId
                rounds[1]["topic"] shouldBe "round 2"
                rounds[1]["started"].shouldNotBeNull()
                rounds[1]["ended"].shouldBeNull()
            }

            test("Get rounds of non-existing game -> 404") {
                // Arrange

                // Act + Assert
                Api.games.getRoundsResponse("non-existing", sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("Game with ID non-existing does not exist"))
            }
        }

        context("POST /v1/games/{gameId}/rounds - Start new round") {
            val (player, sessionId) = Players.createPlayerSession()

            test("Start round -> 200") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                socketIO.joinGame(game.id!!)

                val stub = RoundStub(
                    topic = "Some topic"
                )

                // Act
                val round = Api.games.startRound(game.id!!, stub, sessionId)

                // Assert
                round.gameId shouldBe game.id
                round.started.shouldBeBefore(Instant.now())
                round.ended.shouldBeNull()
                round.topic shouldBe stub.topic
                round.result.shouldBeNull()
            }

            test("Start round with empty topic -> 200") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                val stub = RoundStub(
                    topic = ""
                )

                // Act
                val round = Api.games.startRound(game.id!!, stub, sessionId)

                // Assert
                round.gameId shouldBe game.id
                round.started.shouldBeBefore(Instant.now())
                round.ended.shouldBeNull()
                round.topic shouldBe stub.topic
                round.result.shouldBeNull()
            }

            test("Start round with non-existing game -> 404") {
                // Arrange
                val stub = RoundStub(
                    topic = "Round for non existing game"
                )

                // Act + Assert
                Api.games.startRoundResponse("not-existing", stub, sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("Game with ID not-existing does not exist"))
            }

            test("Start round without session -> 401") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3))
                val stub = RoundStub(
                    topic = "Some topic"
                )

                // Act + Assert
                Api.games.startRoundResponse(game.id!!, stub, sessionId = null)
                    .then()
                    .statusCode(401)
            }

            test("Start round as player that did not join -> 401") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                val (otherPlayer, otherSessionId) = Players.createPlayerSession()

                val stub = RoundStub(
                    topic = "Some topic"
                )

                // Act + Assert
                Api.games.startRoundResponse(game.id!!, stub, sessionId = otherSessionId)
                    .then()
                    .statusCode(403)
                    .body(containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
            }

            test("Start round while one still ongoing -> 409") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                Api.games.startRound(game.id!!, RoundStub("First Round"), sessionId)

                // Act + Assert
                Api.games.startRoundResponse(game.id!!, RoundStub("Second Round"), sessionId)
                    .then()
                    .statusCode(409)
                    .body(containsString("is still ongoing."))
            }
        }

        context("GET /v1/games/{gameId}/rounds/current - Get current round") {
            val (player, sessionId) = Players.createPlayerSession()

            test("Get for game with ongoing round -> 200") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                Api.games.joinGame(game.id!!, sessionId, null)
                val round1 = Api.games.startRound(game.id!!, RoundStub("Round 1"), sessionId)
                Api.games.endRound(game.id!!, round1.id!!, sessionId)
                val round2 = Api.games.startRound(game.id!!, RoundStub("Round 2"), sessionId)

                // Act
                val currentRound = Api.games.getCurrentRound(game.id!!, sessionId)

                // Assert
                currentRound.id shouldBe round2.id
                currentRound.topic shouldBe round2.topic
                currentRound.isFinished() shouldBe false
            }

            test("Get for game with only finished rounds -> 404") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                Api.games.joinGame(game.id!!, sessionId, null)
                val round = Api.games.startRound(game.id!!, RoundStub("Round 1"), sessionId)
                Api.games.endRound(game.id!!, round.id!!, sessionId)

                // Act + Assert
                Api.games.getCurrentRoundResponse(game.id!!, sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("No round is currently ongoing"))
            }

            test("Get for game without rounds -> 404") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                Api.games.joinGame(game.id!!, sessionId, null)

                // Act + Assert
                Api.games.getCurrentRoundResponse(game.id!!, sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("No round is currently ongoing"))
            }

            test("Get for non-existing game -> 404") {
                // Arrange

                // Act + Assert
                Api.games.getCurrentRoundResponse("non-existing", sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("Game with ID non-existing does not exist"))
            }

            test("Get for game the player did not join -> 403") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                val (otherPlayer, otherSessionId) = Players.createPlayerSession()

                // Act + Assert
                Api.games.getCurrentRoundResponse(game.id!!, otherSessionId)
                    .then()
                    .statusCode(403)
                    .body(containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
            }
        }

        context("DELETE /api/v1/games/{gameId}/rounds/{roundId} - End current round") {
            val (player, sessionId) = Players.createPlayerSession()

            test("End round without votes -> 200") {
                // Arrange
                val game = createGame(
                    name = "some-game",
                    password = null,
                    cards = listOf(1, 2, 3),
                    players = listOf(player)
                )
                val round = Api.games.startRound(game.id!!, RoundStub("topic"), sessionId)

                val expectedStats = RoundResults(
                    votes = listOf(),
                    minVotes = listOf(),
                    maxVotes = listOf(),
                    averageVote = null,
                    variance = null,
                    minVoteValue = null,
                    maxVoteValue = null,
                    suggestedCard = null
                )

                // Act
                val endedRound = Api.games.endRound(game.id!!, round.id!!, sessionId)

                // Assert
                endedRound.id shouldBe round.id
                endedRound.topic shouldBe round.topic
                endedRound.started.shouldNotBeNull()
                endedRound.ended.shouldNotBeNull()
                endedRound.result shouldBe expectedStats
            }

            test("End round with votes -> 200") {
                // Arrange
                val game = createGame(
                    name = "some-game",
                    password = null,
                    cards = listOf(1, 2, 3),
                    players = listOf(player)
                )
                val round = Api.games.startRound(game.id!!, RoundStub("topic"), sessionId)

                val voteAlice = Vote(
                    gameId = game.id!!,
                    roundId = round.id!!,
                    player = Player(name = "Alice", gameIds = mutableListOf(game.id!!)),
                    card = Card(1)
                )
                val voteBob = Vote(
                    gameId = game.id!!,
                    roundId = round.id!!,
                    player = Player(name = "Bob", gameIds = mutableListOf(game.id!!)),
                    card = Card(2)
                )
                val voteCharlie = Vote(
                    gameId = game.id!!,
                    roundId = round.id!!,
                    player = Player(name = "Charlie", gameIds = mutableListOf(game.id!!)),
                    card = Card(3)
                )
                val voteDolly = Vote(
                    gameId = game.id!!,
                    roundId = round.id!!,
                    player = Player(name = "Dolly", gameIds = mutableListOf(game.id!!)),
                    card = Card(3)
                )
                val allVotes = listOf(voteAlice, voteBob, voteCharlie, voteDolly)
                voteRepository.saveAll(allVotes)

                // Act
                val endedRound = Api.games.endRound(game.id!!, round.id!!, sessionId)

                // Assert
                endedRound.id shouldBe round.id
                endedRound.topic shouldBe round.topic
                endedRound.started.shouldNotBeNull()
                endedRound.ended.shouldNotBeNull()

                val result = endedRound.result
                result.shouldNotBeNull()
                result.votes.map { it.copy(id = null) } shouldContainExactlyInAnyOrder allVotes
                result.minVotes.map { it.copy(id = null) } shouldBe listOf(voteAlice)
                result.maxVotes.map { it.copy(id = null) } shouldContainExactlyInAnyOrder listOf(voteCharlie, voteDolly)
                result.minVoteValue shouldBe 1
                result.maxVoteValue shouldBe 3
                result.averageVote shouldBe 2.25
                result.variance shouldBe 0.82915619758885
                result.suggestedCard shouldBe Card(3)
            }

            test("End round of non-existing game -> 404") {
                // Arrange

                // Act + Assert
                Api.games.endRoundResponse("not-existing", "not-existing", sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("Game with ID not-existing does not exist"))
            }

            test("End non-existing round -> 404") {
                // Arrange
                val game = createGame(
                    name = "some-game",
                    password = null,
                    cards = listOf(1, 2, 3),
                    players = listOf(player)
                )

                // Act + Assert
                Api.games.endRoundResponse(game.id!!, "not-existing", sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("Round with ID not-existing does not exist"))
            }

            test("End round of game where player did not join -> 400") {
                // Arrange
                val game = createGame(
                    name = "some-game",
                    password = null,
                    cards = listOf(1, 2, 3),
                    players = listOf(player)
                )
                val round = Api.games.startRound(game.id!!, RoundStub("topic"), sessionId)
                val (otherPlayer, otherSessionId) = Api.players.createPlayerSession()

                // Act + Assert
                Api.games.endRoundResponse(game.id!!, round.id!!, otherSessionId)
                    .then()
                    .statusCode(403)
                    .body(containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
            }

            test("End round that was already ended -> 400") {
                // Arrange
                val game = createGame(
                    name = "some-game",
                    password = null,
                    cards = listOf(1, 2, 3),
                    players = listOf(player)
                )
                val round = Api.games.startRound(game.id!!, RoundStub("topic"), sessionId)
                Api.games.endRound(game.id!!, round.id!!, sessionId)

                // Act + Assert
                Api.games.endRoundResponse(game.id!!, round.id!!, sessionId)
                    .then()
                    .statusCode(400)
                    .body(containsString("Round with ID '${round.id}' is already finished."))
            }
        }
    }
}