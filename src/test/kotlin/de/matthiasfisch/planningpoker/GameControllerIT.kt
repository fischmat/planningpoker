package de.matthiasfisch.planningpoker

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.util.CleanupExtension
import de.matthiasfisch.planningpoker.util.RestAssuredExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.mapper.ObjectMapperType
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class GameControllerIT(
    @LocalServerPort val serverPort: Int
): FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, RestAssuredExtension(serverPort))
    val cleanup = extension(CleanupExtension())

    @Autowired
    private lateinit var gameRepository: GameRepository

    fun createGame(name: String, password: String?, cards: List<Int>): Game {
        return gameRepository.save(Game(
            name = name,
            password = password,
            playableCards = cards.map { Card(it) }
        )).also {
            cleanup.addTask { gameRepository.delete(it) }
        }
    }

    fun createPlayerSession(): Pair<Player, String> {
        val response = RestAssured.given()
            .header("Content-Type", "application/json")
            .body(PlayerStub("it-player"), ObjectMapperType.JACKSON_2)
            .`when`()
            .request(Method.POST, "/v1/players")
            .then()
            .extract()
        return response.`as`(Player::class.java) to response.cookie(Api.sessionCookieName)
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
                result.players shouldBe listOf(player)
                result.rounds.shouldBeEmpty()
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
                result.players shouldBe listOf(player)
                result.rounds.shouldBeEmpty()
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
    }
}