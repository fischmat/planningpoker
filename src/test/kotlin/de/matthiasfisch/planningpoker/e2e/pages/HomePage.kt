package de.matthiasfisch.planningpoker.e2e.pages

import io.kotest.matchers.shouldBe
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions

class HomePage(driver: WebDriver, config: SeleniumConfig): Page(driver, config) {
    fun goto(): HomePage {
        driver.navigate().to(url("/"))
        driver.title shouldBe "Planning Poker"
        return this
    }

    fun startGame() {
        driver.findElement(By.id("start-game-card")).click()
        wait().until(ExpectedConditions.elementToBeClickable(By.id("session-name")))
    }
}