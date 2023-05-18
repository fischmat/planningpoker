package de.matthiasfisch.planningpoker.e2e.pages

import org.openqa.selenium.support.ui.WebDriverWait
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

abstract class Page(val app: App, val config: SeleniumConfig) {

    fun url(path: String) = with(config) {
        "$scheme://$host:$port$path"
    }

    fun wait(): WebDriverWait {
        return WebDriverWait(app.driver, 10.seconds.toJavaDuration())
    }
}

data class SeleniumConfig(
    val scheme: String,
    val host: String,
    val port: Int
)