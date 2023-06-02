package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.support.ui.ExpectedConditions

class EditPlayer(app: App, config: SeleniumConfig): Page(app, config) {

    fun awaitPagePresent() {
        wait().until {
            it.findElement(By.id("player-name")) != null || it.findElement(By.id("game-title")) != null
        }
    }

    fun enterName(text: String): EditPlayer {
        app.driver.findElement(By.id("player-name")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun submit() {
        wait().until { ExpectedConditions.elementToBeClickable(By.id("submit-player")) }
        app.driver.findElement(By.id("submit-player")).click()
    }
}