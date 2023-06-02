package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class GamePage(driver: WebDriver, config: SeleniumConfig): Page(driver, config) {

    fun awaitPagePresent() {
        wait().until {
            it.findElement(By.className("round-info")) != null
        }
    }

    fun getWindowTitle(): String? {
        return driver.title
    }
}