package de.matthiasfisch.planningpoker

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.controller.Players.createPlayerSession
import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.util.CleanupExtension
import de.matthiasfisch.planningpoker.util.RestAssuredExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class GameControllerIT(
    @LocalServerPort val serverPort: Int
): FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, RestAssuredExtension(serverPort))
    val cleanup = extension(CleanupExtension())

    @Autowired
    private lateinit var gameRepository: GameRepository

    @Autowired
    private lateinit var playerRepository: PlayerRepository

    @Autowired
    private lateinit var voteRepository: VoteRepository

    fun createGame(name: String, password: String?, cards: List<Int>, players: List<Player> = listOf()): Game {
        return gameRepository.save(Game(
            name = name,
            password = password,
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
        context("GET /v1/games") {
            test("Get games and no games exist -> 200") {
                // Arrange
                val expectedPage = PagedResult<Game>(
                    items = listOf(),
                    page = 0,
                    pageSize = 10,
                    totalPages = 0
                )

                // Act
                val page = Api.games.getPage(0, 10)

                // Assert
                page shouldBe expectedPage
            }

            test("Get pages -> 200 - correct items") {
                // Arrange
                val game1 = createGame("game 1", "1234", listOf(1, 2, 3))
                val game2 = createGame("game 2", null, listOf(1, 2))
                val game3 = createGame("game 3", null, listOf(3, 4, 5, 6))
                val game4 = createGame("game 4", "1234", listOf(1, 2, 3))
                val game5 = createGame("game 5", null, listOf(1, 2, 3))

                // Act
                val firstPage = Api.games.getPage(0, 3)
                val secondPage = Api.games.getPage(1, 3)
                val thirdPage = Api.games.getPage(2, 3)

                // Assert
                firstPage.page shouldBe 0
                firstPage.pageSize shouldBe 3
                firstPage.totalPages shouldBe 2
                firstPage.items.map { it["id"] } shouldBe listOf(game1.id, game2.id, game3.id)
                firstPage.items.map { it["password"] } shouldBe listOf("****", null, null)

                secondPage.page shouldBe 1
                secondPage.pageSize shouldBe 3
                secondPage.totalPages shouldBe 2
                secondPage.items.map { it["id"] } shouldBe listOf(game4.id, game5.id)
                secondPage.items.map { it["password"] } shouldBe listOf("****", null)

                thirdPage.page shouldBe 2
                thirdPage.pageSize shouldBe 3
                thirdPage.totalPages shouldBe 2
                thirdPage.items.shouldBeEmpty()
            }
        }

        context("GET /v1/games/{gameId}") {
            test("Get game without password -> 200") {
                // Arrange
                val game = createGame("test", null, listOf(1, 2, 3))

                // Act
                val result = Api.games.getGame(game.id!!)

                // Assert
                result shouldBe game
            }

            test("Get game with password -> 200 - password masked") {
                // Arrange
                val game = createGame("test", "some password", listOf(1, 2, 3))

                // Act
                val result = Api.games.getGame(game.id!!)

                // Assert
                result.id shouldBe game.id
                result.name shouldBe game.name
                result.playableCards shouldBe game.playableCards
                result.password shouldBe "****"
            }

            test("Get non-existing game -> 404") {
                // Arrange

                // Act + Assert
                Api.games.getGameResponse("not-existing")
                    .then()
                    .statusCode(404)
            }
        }

        context("POST /v1/games") {
            val (player, sessionId) = createPlayerSession()

            test("Create game without password -> 200") {
                // Arrange
                val stub = GameStub(
                    name = "My test game without password",
                    password = null,
                    playableCards = listOf(Card(1), Card(2), Card(5))
                )

                // Act
                val result = Api.games.createGame(stub, sessionId)

                // Assert
                cleanup.addTask { gameRepository.deleteById(result.id!!) }

                result.id.shouldNotBeBlank()
                result.name shouldBe stub.name
                result.playableCards shouldBe stub.playableCards
                result.password.shouldBeNull()

                Api.players.getPlayer(player.id!!).gameIds shouldContain result.id
            }

            test("Create game with password -> 200") {
                // Arrange
                val stub = GameStub(
                    name = "My test game with password",
                    password = "some password",
                    playableCards = listOf(Card(1), Card(2), Card(5))
                )

                // Act
                val result = Api.games.createGame(stub, sessionId)

                // Assert
                cleanup.addTask { gameRepository.deleteById(result.id!!) }

                result.id.shouldNotBeBlank()
                result.name shouldBe stub.name
                result.playableCards shouldBe stub.playableCards
                result.password shouldBe "****"

                Api.players.getPlayer(player.id!!).gameIds shouldContain result.id
            }

            test("Create game without session -> 401") {
                // Arrange
                val stub = GameStub(
                    name = "My test game",
                    password = null,
                    playableCards = listOf(Card(1), Card(2), Card(5))
                )

                // Act + Assert
                Api.games.createGameResponse(stub, null)
                    .then()
                    .statusCode(401)
            }

            test("Create game with empty name -> 400") {
                // Arrange
                val stub = GameStub(
                    name = "",
                    password = "some password",
                    playableCards = listOf(Card(1), Card(2), Card(5))
                )

                // Act + Assert
                Api.games.createGameResponse(stub, sessionId)
                    .then()
                    .statusCode(400)
                    .body(Matchers.containsString("Name of game must not be blank."))
            }

            test("Create game without cards -> 400") {
                // Arrange
                val stub = GameStub(
                    name = "Some game without cards",
                    password = "some password",
                    playableCards = listOf()
                )

                // Act + Assert
                Api.games.createGameResponse(stub, sessionId)
                    .then()
                    .statusCode(400)
                    .body(Matchers.containsString("No playable cards are set for the game."))
            }

            test("Create game with same card twice -> 400") {
                // Arrange
                val stub = GameStub(
                    name = "Some game with duplicated cards",
                    password = "some password",
                    playableCards = listOf(Card(1), Card(2), Card(2), Card(3), Card(3))
                )

                // Act + Assert
                Api.games.createGameResponse(stub, sessionId)
                    .then()
                    .statusCode(400)
                    .body(Matchers.containsString("Playable cards are duplicated: 2, 3"))
            }
        }

        context("GET /v1/games/{gameId}/rounds") {
            val (player, sessionId) = createPlayerSession()
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
                    .body(Matchers.containsString("Game with ID non-existing does not exist"))
            }
        }

        context("POST /v1/games/{gameId}/rounds") {
            val (player, sessionId) = createPlayerSession()

            test("Start round -> 200") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
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
                    .body(Matchers.containsString("Game with ID not-existing does not exist"))
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
                val (otherPlayer, otherSessionId) = createPlayerSession()

                val stub = RoundStub(
                    topic = "Some topic"
                )

                // Act + Assert
                Api.games.startRoundResponse(game.id!!, stub, sessionId = otherSessionId)
                    .then()
                    .statusCode(403)
                    .body(Matchers.containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
            }

            test("Start round while one still ongoing -> 409") {
                // Arrange
                val game = createGame("some-game", null, listOf(1, 2, 3), listOf(player))
                Api.games.startRound(game.id!!, RoundStub("First Round"), sessionId)

                // Act + Assert
                Api.games.startRoundResponse(game.id!!, RoundStub("Second Round"), sessionId)
                    .then()
                    .statusCode(409)
                    .body(Matchers.containsString("is still ongoing."))
            }
        }

        context("DELETE /api/v1/games/{gameId}/rounds/{roundId}") {
            val (player, sessionId) = createPlayerSession()

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
                    .body(Matchers.containsString("Game with ID not-existing does not exist"))
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
                    .body(Matchers.containsString("Round with ID not-existing does not exist"))
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
                    .body(Matchers.containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
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
                    .body(Matchers.containsString("Round with ID '${round.id}' is already finished."))
            }
        }
    }
}