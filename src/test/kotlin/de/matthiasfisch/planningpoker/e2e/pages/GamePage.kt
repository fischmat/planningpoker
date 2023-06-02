package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.By

class GamePage(app: App, config: SeleniumConfig): Page(app, config) {

    fun awaitPagePresent() {
        wait().until {
            it.findElement(By.className("round-info")) != null
        }
    }

    fun getWindowTitle(): String? {
        return app.driver.title
    }
}