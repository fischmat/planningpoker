package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions

class EditPlayer(app: App, config: SeleniumConfig): Page(app, config) {

    fun isPresent(): Boolean {
        return app.driver.findElement(By.id("player-name")) != null
    }

    fun enterName(text: String): EditPlayer {
        app.driver.findElement(By.id("player-name")).run {
            clear()
            sendKeys(text)
            app.driver.
        }
        return this
    }

    fun awaitSubmitEnabled() {
        wait().until(ExpectedConditions.elementToBeClickable(By.id("submit-player")))
    }

    fun submit() {
        app.driver.findElement(By.id("submit-player")).click()
    }
}