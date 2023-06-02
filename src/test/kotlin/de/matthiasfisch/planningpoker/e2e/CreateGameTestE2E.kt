package de.matthiasfisch.planningpoker.e2e

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.e2e.pages.App
import de.matthiasfisch.planningpoker.e2e.pages.SeleniumConfig
import io.github.bonigarcia.wdm.WebDriverManager
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldNotBeTypeOf
import kotlinx.coroutines.delay
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it", "e2e")
class CreateGameTestE2E(
    @LocalServerPort val serverPort: Int
) : FunSpec() {
    init {
        lateinit var driver: WebDriver
        lateinit var app: App

        val clientContainer = install(TestContainerExtension("docker.matthias-fisch.de/poker-client:latest")) {
            withExposedPorts(80)
            withEnv("BASE_PATH", "http://localhost:$serverPort")
            withImagePullPolicy { true }
        }

        beforeSpec {
            clientContainer.start()

            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions()
            options.setHeadless(false)
            options.addArguments("--remote-allow-origins=*", "--window-size=1920,1500")
            driver = ChromeDriver(options)

            app = App(driver, SeleniumConfig("http", "localhost", clientContainer.firstMappedPort))
        }

        beforeEach {
            driver.manage().deleteAllCookies()
        }

        test("Create a new game without a player session") {
            // Arrange
            val sessionName = "E2E test session"
            val playerName = "E2E Test User"
            val cards = listOf(1, 2, 3, 4)

            // Act + Assert
            app.home
                .goto()
                .startGame()

            app.setupGame.awaitPagePresent()
            app.setupGame.canSubmit() shouldBe false
            app.setupGame.enterSessionName(sessionName)
            app.setupGame.canSubmit() shouldBe true
            app.setupGame.enterCards("")
            app.setupGame.canSubmit() shouldBe false
            app.setupGame.enterCards(cards.joinToString(", "))
            app.setupGame.canSubmit() shouldBe true
            app.setupGame.submit()

            app.editPlayer.awaitPagePresent()
            app.editPlayer.enterName(playerName)
            app.editPlayer.submit()

            app.game.awaitPagePresent()
            app.game.getWindowTitle() shouldStartWith sessionName
        }
    }
}