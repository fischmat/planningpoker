package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.support.ui.ExpectedConditions

class EnterPassword(app: App, config: SeleniumConfig): Page(app, config) {

    fun enterPassword(text: String): EnterPassword {
        wait().until { ExpectedConditions.visibilityOf(app.driver.findElement(By.id("game-password"))) }
        app.driver.findElement(By.id("game-password")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun submit() {
        wait().until { ExpectedConditions.elementToBeClickable(By.id("submit")) }
        app.driver.findElement(By.id("submit")).click()
    }

    fun isWrongPassword(): Boolean {
        return app.driver.findElement(By.id("wrong-password-box")).isDisplayed
    }
}