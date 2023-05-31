package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.WebDriver

class App(val driver: WebDriver, config: SeleniumConfig) {
    val home = Home(this, config)
    val setupGame = SetupGame(this, config)
    val editPlayer = EditPlayer(this, config)
    val enterPassword = EnterPassword(this, config)
    val game = GamePage(this, config)
}