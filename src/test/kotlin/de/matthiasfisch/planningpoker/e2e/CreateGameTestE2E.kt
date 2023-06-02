package de.matthiasfisch.planningpoker.e2e

import de.matthiasfisch.planningpoker.util.E2ETestExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it", "e2e")
class CreateGameTestE2E(
    @LocalServerPort val serverPort: Int
) : FunSpec() {
    init {
        val e2e = extension(E2ETestExtension(serverPort))

        test("Create a new game without a player session") {
            // Arrange
            val sessionName = "E2E test session"
            val playerName = "E2E Test User"
            val cards = listOf(1, 2, 3, 4)

            // Act + Assert
            e2e.home
                .goto()
                .startGame()

            e2e.setupGame.awaitPagePresent()
            e2e.setupGame.canSubmit() shouldBe false
            e2e.setupGame.enterSessionName(sessionName)
            e2e.setupGame.canSubmit() shouldBe true
            e2e.setupGame.enterCards("")
            e2e.setupGame.canSubmit() shouldBe false
            e2e.setupGame.enterCards(cards.joinToString(", "))
            e2e.setupGame.canSubmit() shouldBe true
            e2e.setupGame.submit()

            e2e.editPlayer.awaitPagePresent()
            e2e.editPlayer.enterName(playerName)
            e2e.editPlayer.submit()

            e2e.game.awaitPagePresent()
            e2e.game.getWindowTitle() shouldStartWith sessionName
        }
    }
}