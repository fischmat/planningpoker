package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By

class SetupGame(app: App, config: SeleniumConfig): Page(app, config) {

    fun enterSessionName(text: String): SetupGame {
        app.driver.findElement(By.id("session-name")).run {
            clear()
            sendKeys(text)
            sendKeys("\t")
        }
        return this
    }

    fun enterPassword(text: String): SetupGame {
        app.driver.findElement(By.id("session-password")).run {
            clear()
            sendKeys(text)
            sendKeys("\t")
        }
        return this
    }

    fun enterCards(text: String): SetupGame {
        app.driver.findElement(By.id("session-cards")).run {
            clear()
            sendKeys(text)
            sendKeys("\t")
        }
        return this
    }

    fun canSubmit(): Boolean {
        return app.driver.findElement(By.id("session-submit")).isEnabled
    }

    fun submit() {
        app.driver.findElement(By.id("session-submit")).click()
        wait().until {
            it.findElement(By.id("player-name")) != null || it.findElement(By.id("game-title")) != null
        }
    }
}