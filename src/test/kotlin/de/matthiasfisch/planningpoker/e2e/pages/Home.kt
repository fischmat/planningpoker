package de.matthiasfisch.planningpoker.e2e.pages

import io.kotest.matchers.shouldBe
import org.http4k.lens.StringBiDiMappings.url
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions

class Home(app: App, config: SeleniumConfig): Page(app, config) {
    fun goto(): Home {
        app.driver.navigate().to(url("/"))
        app.driver.title shouldBe "Planning Poker"
        return this
    }

    fun startGame(): SetupGame {
        app.driver.findElement(By.id("start-game-card")).click()
        wait().until(ExpectedConditions.elementToBeClickable(By.id("session-name")))
        return app.setupGame
    }
}