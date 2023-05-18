package de.matthiasfisch.planningpoker.integration

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.controller.Players.createPlayerSession
import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.service.PasswordHashingService
import de.matthiasfisch.planningpoker.util.CleanupExtension
import de.matthiasfisch.planningpoker.util.RestAssuredExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.hamcrest.Matchers.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class GamesIT(
    @LocalServerPort val serverPort: Int
): FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, RestAssuredExtension(serverPort))
    val cleanup = extension(CleanupExtension())

    @Autowired
    private lateinit var gameRepository: GameRepository

    @Autowired
    private lateinit var playerRepository: PlayerRepository

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
        context("GET /v1/games - Get games paginated") {
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
                firstPage.items.map { it["hasPassword"] } shouldBe listOf(true, false, false)

                secondPage.page shouldBe 1
                secondPage.pageSize shouldBe 3
                secondPage.totalPages shouldBe 2
                secondPage.items.map { it["id"] } shouldBe listOf(game4.id, game5.id)
                secondPage.items.map { it["hasPassword"] } shouldBe listOf(true, false)

                thirdPage.page shouldBe 2
                thirdPage.pageSize shouldBe 3
                thirdPage.totalPages shouldBe 2
                thirdPage.items.shouldBeEmpty()
            }
        }

        context("GET /v1/games/{gameId} - Get game by ID") {
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
            }

            test("Get non-existing game -> 404") {
                // Arrange

                // Act + Assert
                Api.games.getGameResponse("not-existing")
                    .then()
                    .statusCode(404)
            }
        }

        context("POST /v1/games - Create new game") {
            val (_, sessionId) = createPlayerSession()

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
            }

            test("Password or hash not in response") {
                // Arrange
                val password = "some password"
                val stub = GameStub(
                    name = "My test game with password",
                    password = password,
                    playableCards = listOf(Card(1), Card(2), Card(5))
                )

                // Act + Assert
                Api.games.createGameResponse(stub, sessionId)
                    .then()
                    .statusCode(200)
                    .body(
                        not(containsString(password)),
                        not(containsString(passwordHashing.createIntermediate(password))),
                        not(containsString(passwordHashing.encodePlaintext(password))),
                    ).body("hasPassword", `is`(true))
            }

            test("Create game without session -> 200") {
                // Arrange
                val stub = GameStub(
                    name = "My test game",
                    password = null,
                    playableCards = listOf(Card(1), Card(2), Card(5))
                )

                // Act + Assert
                Api.games.createGameResponse(stub, null)
                    .then()
                    .statusCode(200)
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
                    .body(containsString("Name of game must not be blank."))
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
                    .body(containsString("No playable cards are set for the game."))
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
                    .body(containsString("Playable cards are duplicated: 2, 3"))
            }
        }

        context("GET /v1/games/{gameId}/players - Get players in game") {
            val (player, sessionId) = createPlayerSession()

            test("Get for game active game -> 200") {
                // Arrange
                val game = createGame("test", null, listOf(1, 2, 3))
                Api.games.joinGame(game.id!!, sessionId, null)

                // Act
                val players = Api.games.getPlayersInGame(game.id!!, sessionId)

                // Assert
                players.map { it["id"].toString() } shouldBe listOf(player.id)
            }

            test("Get for non-existing game -> 404") {
                // Arrange

                // Act + Assert
                Api.games.getPlayersInGameResponse("non-existing", sessionId)
                    .then()
                    .statusCode(404)
                    .body(containsString("Game with ID non-existing does not exist"))
            }

            test("Get for game the player did not join -> 403") {
                // Arrange
                val game = createGame("test", null, listOf(1, 2, 3))

                // Act + Assert
                Api.games.getPlayersInGameResponse(game.id!!, sessionId)
                    .then()
                    .statusCode(403)
                    .body(containsString("Player did not join game ${game.id}"))
            }
        }

        context("POST /api/v1/games/{gameId}/players - Join a game") {
            test("Join unprotected game -> 200") {
                // Arrange
                val game = createGame("test", null, listOf(1, 2, 3))
                val (player, sessionId) = createPlayerSession()

                // Act
                val result = Api.games.joinGame(game.id!!, sessionId, null)

                // Assert
                result shouldBe player.copy(gameIds = mutableListOf(game.id!!))
            }

            test("Join protected game with password -> 200") {
                // Arrange
                val password = "super secret password"
                val game = createGame("test", password, listOf(1, 2, 3))
                val (player, sessionId) = createPlayerSession()

                val bearerToken = passwordHashing.createIntermediate(password)

                // Act
                val result = Api.games.joinGame(game.id!!, sessionId, bearerToken)

                // Assert
                result shouldBe player.copy(gameIds = mutableListOf(game.id!!))
            }

            test("Join game without session -> 401") {
                // Arrange
                val game = createGame("test", null, listOf(1, 2, 3))

                // Act + Assert
                Api.games.joinGameResponse(game.id!!, null, null)
                    .then()
                    .statusCode(401)
            }

            test("Join protected game without password -> 401") {
                // Arrange
                val password = "super secret password"
                val game = createGame("test", password, listOf(1, 2, 3))
                val (_, sessionId) = createPlayerSession()

                // Act + Assert
                Api.games.joinGameResponse(game.id!!, sessionId, null)
                    .then()
                    .statusCode(401)
                    .header("WWW-Authenticate", "Bearer")
            }

            test("Join protected game with wrong password -> 401") {
                // Arrange
                val password = "super secret password"
                val game = createGame("test", password, listOf(1, 2, 3))
                val (_, sessionId) = createPlayerSession()

                // Act + Assert
                Api.games.joinGameResponse(game.id!!, sessionId, "wrongpassword")
                    .then()
                    .statusCode(401)
                    .header("WWW-Authenticate", "Bearer")
            }
        }
    }
}