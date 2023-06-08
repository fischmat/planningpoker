package de.matthiasfisch.planningpoker.integration

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.controller.Players
import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.util.RestAssuredExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import org.hamcrest.Matchers.containsString
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class VotesIT(
    @LocalServerPort val serverPort: Int
) : FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, RestAssuredExtension(serverPort))

    val playableCards = listOf(Card(1), Card(2), Card(3), Card(4))

    lateinit var game: Game
    lateinit var round: Round
    lateinit var player: Player
    lateinit var sessionId: String

    override suspend fun beforeEach(testCase: TestCase) {
        Players.createPlayerSession().let {
            player = it.player
            sessionId = it.sessionId
        }
        game = Api.games.createGame(GameStub("VotesIT", null, playableCards), sessionId)
        round = Api.games.startRound(game.id!!, RoundStub("first round"), sessionId)
    }

    init {
        context("POST /v1/games/{gameId}/rounds/{roundId}/votes") {
            test("Submit valid vote -> 200") {
                // Arrange
                val card = Card(2)

                // Act
                val vote = Api.games.submitVote(game.id!!, round.id!!, card, sessionId)

                // Assert
                vote.gameId shouldBe game.id
                vote.roundId shouldBe round.id
                vote.player.id shouldBe player.id
                vote.card shouldBe card
            }

            test("Submit vote twice -> 200") {
                // Arrange
                val firstCard = Card(2)
                val secondCard = Card(3)

                // Act
                Api.games.submitVote(game.id!!, round.id!!, firstCard, sessionId)
                val vote = Api.games.submitVote(game.id!!, round.id!!, secondCard, sessionId)

                // Assert
                vote.gameId shouldBe game.id
                vote.roundId shouldBe round.id
                vote.player.id shouldBe player.id
                vote.card shouldBe secondCard

                val votes = Api.games.getVotes(game.id!!, round.id!!, sessionId)
                votes.shouldHaveSize(1)
                (votes.first()["card"] as Map<String, Any>).shouldContain("value", secondCard.value)
            }

            test("Submit invalid vote -> 400") {
                // Arrange
                val invalidCard = Card(42)

                // Act + Assert
                Api.games.submitVoteResponse(game.id!!, round.id!!, invalidCard, sessionId)
                    .then()
                    .statusCode(400)
                    .body(containsString("Card with value ${invalidCard.value} can not be played in game ${game.id}."))
            }

            test("Submit vote without session -> 401") {
                // Arrange

                // Act + Assert
                Api.games.submitVoteResponse(game.id!!, round.id!!, Card(2), sessionId = null)
                    .then()
                    .statusCode(401)
            }

            test("Submit vote while not in game -> 403") {
                // Arrange
                val (otherPlayer, otherSessionId) = Players.createPlayerSession()

                // Act + Assert
                Api.games.submitVoteResponse(game.id!!, round.id!!, Card(2), otherSessionId)
                    .then()
                    .statusCode(403)
                    .body(containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
            }
        }

        context("DELETE /v1/games/{gameId}/rounds/{roundId}/votes/mine") {
            test("Revoke existing vote -> 200") {
                // Arrange
                Api.games.submitVote(game.id!!, round.id!!, Card(2), sessionId)

                // Act
                Api.games.revokeVoteResponse(game.id!!, round.id!!, sessionId)
                    .then()
                    .statusCode(200)

                Api.games.getVotes(game.id!!, round.id!!, sessionId).shouldBeEmpty()
            }

            test("Revoke non-existing vote -> 200") {
                // Arrange

                // Act
                Api.games.revokeVoteResponse(game.id!!, round.id!!, sessionId)
                    .then()
                    .statusCode(200)

                Api.games.getVotes(game.id!!, round.id!!, sessionId).shouldBeEmpty()
            }

            test("Revoke vote without session -> 401") {
                // Arrange

                // Act
                Api.games.revokeVoteResponse(game.id!!, round.id!!, sessionId = null)
                    .then()
                    .statusCode(401)
            }

            test("Revoke vote while not in game -> 403") {
                // Arrange
                val (otherPlayer, otherSessionId) = Players.createPlayerSession()

                // Act
                Api.games.revokeVoteResponse(game.id!!, round.id!!, otherSessionId)
                    .then()
                    .statusCode(403)
                    .body(containsString("Player with ID '${otherPlayer.id}' is not part of game '${game.id}'"))
            }
        }
    }
}