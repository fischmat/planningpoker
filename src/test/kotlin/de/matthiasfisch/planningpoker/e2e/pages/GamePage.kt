package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions

class GamePage(app: App, config: SeleniumConfig): Page(app, config) {

    fun getTitle(): String? {
        wait().until { ExpectedConditions.visibilityOf(app.driver.findElement(By.id("game-title"))) }
        return app.driver.findElement(By.id("game-title")).text
    }
}