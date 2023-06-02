package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions

class EnterPasswordPage(driver: WebDriver, config: SeleniumConfig): Page(driver, config) {

    fun enterPassword(text: String): EnterPasswordPage {
        wait().until { ExpectedConditions.visibilityOf(driver.findElement(By.id("game-password"))) }
        driver.findElement(By.id("game-password")).run {
            clear()
            sendKeys(text)
            sendKeys(Keys.TAB)
        }
        return this
    }

    fun submit() {
        wait().until { ExpectedConditions.elementToBeClickable(By.id("submit")) }
        driver.findElement(By.id("submit")).click()
    }

    fun isWrongPassword(): Boolean {
        return driver.findElement(By.id("wrong-password-box")).isDisplayed
    }
}