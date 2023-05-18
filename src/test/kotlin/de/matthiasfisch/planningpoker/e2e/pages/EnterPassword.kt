package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By

class EnterPassword(app: App, config: SeleniumConfig): Page(app, config) {
    fun isPresent(): Boolean {
        return app.driver.findElement(By.id("game-password")) != null
    }
}