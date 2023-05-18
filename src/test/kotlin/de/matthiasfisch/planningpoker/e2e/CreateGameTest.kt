package de.matthiasfisch.planningpoker.e2e

import de.matthiasfisch.planningpoker.e2e.pages.App
import de.matthiasfisch.planningpoker.e2e.pages.SeleniumConfig
import de.matthiasfisch.planningpoker.model.Game
import io.github.bonigarcia.wdm.WebDriverManager
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import kotlin.time.Duration.Companion.seconds

class CreateGameTest: FunSpec() {
    init {
        lateinit var driver: WebDriver
        lateinit var app: App

        beforeSpec {
            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions()
            options.setHeadless(true)
            options.addArguments("--remote-allow-origins=*")
            driver = ChromeDriver(options)

            app = App(driver, SeleniumConfig("http", "localhost", 5173))
        }

        test("Create a new game with password") {
            app.home.goto()
                .startGame()
                .apply { canSubmit() shouldBe false }
                .enterSessionName("E2E - Create Game Test")
                .apply { canSubmit() shouldBe true }
                .enterPassword("secret password")
                .enterCards("")
                .apply { canSubmit() shouldBe false }
                .enterCards("1, 2, 3, 4")
                .apply { wait().until { canSubmit() } }
                .submit()

            app.editPlayer
                .apply { isPresent() shouldBe true }
                .enterName("E2E Player")
            app.editPlayer.awaitSubmitEnabled()
            app.editPlayer.submit()

            eventually(2.seconds) {
                app.enterPassword.isPresent() shouldBe true
            }
        }
    }
}