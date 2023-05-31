package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.Keys

class SetupGame(app: App, config: SeleniumConfig): Page(app, config) {

    fun enterSessionName(text: String): SetupGame {
        app.driver.findElement(By.id("session-name")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun enterPassword(text: String): SetupGame {
        app.driver.findElement(By.id("session-password")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun enterCards(text: String): SetupGame {
        app.driver.findElement(By.id("session-cards")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun submit() {
        app.driver.findElement(By.id("session-submit")).click()
    }
}