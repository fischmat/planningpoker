package de.matthiasfisch.planningpoker.e2e

import de.matthiasfisch.planningpoker.e2e.pages.App
import de.matthiasfisch.planningpoker.e2e.pages.SeleniumConfig
import io.github.bonigarcia.wdm.WebDriverManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

class CreateGameTestE2E : FunSpec() {
    init {
        lateinit var driver: WebDriver
        lateinit var app: App

        beforeSpec {
            WebDriverManager.chromedriver().setup()
            val options = ChromeOptions()
            options.setHeadless(true)
            options.addArguments("--remote-allow-origins=*", "--window-size=1920,1500")
            driver = ChromeDriver(options)

            app = App(driver, SeleniumConfig("http", "localhost", 5173))
        }

        test("Create a new game with password") {
            app.home.goto()
                .startGame()
                .enterSessionName("E2E - Create Game Test")
                .enterPassword("secret password")
                .enterCards("")
                .enterCards("1, 2, 3, 4")
                .submit()

            app.editPlayer.awaitPagePresent()
            app.editPlayer.enterName("E2E Player")
            app.editPlayer.awaitSubmitEnabled()
            app.editPlayer.submit()

            app.game.getTitle() shouldBe "E2E - Create Game Test"
        }
    }
}