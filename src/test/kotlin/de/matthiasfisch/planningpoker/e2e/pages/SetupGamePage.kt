package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver

class SetupGamePage(driver: WebDriver, config: SeleniumConfig): Page(driver, config) {

    fun enterSessionName(text: String): SetupGamePage {
        driver.findElement(By.id("session-name")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun enterPassword(text: String): SetupGamePage {
        driver.findElement(By.id("session-password")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun enterCards(text: String): SetupGamePage {
        driver.findElement(By.id("session-cards")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun submit() {
        driver.findElement(By.id("session-submit")).click()
    }

    fun canSubmit(): Boolean {
        return driver.findElement(By.id("session-submit")).isEnabled
    }

    fun awaitPagePresent(): SetupGamePage {
        wait().until {
            it.findElement(By.id("session-name")) != null
        }
        return this
    }
}