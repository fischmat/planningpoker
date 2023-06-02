package de.matthiasfisch.planningpoker.util

import de.matthiasfisch.planningpoker.e2e.pages.*
import io.github.bonigarcia.wdm.WebDriverManager
import io.kotest.core.extensions.install
import io.kotest.core.listeners.BeforeEachListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.extensions.testcontainers.TestContainerExtension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.testcontainers.containers.GenericContainer

class E2ETestExtension(private val serverPort: Int): BeforeSpecListener, BeforeEachListener {
    companion object {
        /**
         * Whether the E2E tests should run in headless mode (default: true)
         */
        const val ENV_VAR_HEADLESS = "E2E_HEADLESS"

        /**
         * If set, no client container is spawned.
         * Instead, the local client at http://localhost:5173 will be used.
         */
        const val ENV_VAR_USE_LOCAL_CLIENT = "E2E_LOCAL_CLIENT"

        /**
         * Allows specifying the docker image for the frontend.
         */
        const val ENV_VAR_CLIENT_IMAGE = "E2E_POKER_CLIENT_IMAGE"

        private const val DEFAULT_CLIENT_IMAGE = "docker.matthias-fisch.de/poker-client:latest"

        private const val LOCAL_DEV_CLIENT_PORT = 5173
    }

    private lateinit var clientContainer: GenericContainer<*>
    private lateinit var driver: WebDriver

    lateinit var home: HomePage
    lateinit var setupGame: SetupGamePage
    lateinit var editPlayer: EditPlayerPage
    lateinit var enterPassword: EnterPasswordPage
    lateinit var game: GamePage

    override suspend fun beforeSpec(spec: Spec) {
        val frontendPort = if (System.getenv(ENV_VAR_USE_LOCAL_CLIENT) == null) {
            clientContainer = startClientContainer(spec)
            clientContainer.firstMappedPort
        } else {
            LOCAL_DEV_CLIENT_PORT
        }

        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions()
        options.setHeadless(System.getenv(ENV_VAR_HEADLESS)?.let { it != "false" }?: true)
        options.addArguments("--remote-allow-origins=*", "--window-size=1920,1500")
        driver = ChromeDriver(options)

        val config = SeleniumConfig("http", "localhost", frontendPort)
        home = HomePage(driver, config)
        setupGame = SetupGamePage(driver, config)
        editPlayer = EditPlayerPage(driver, config)
        enterPassword = EnterPasswordPage(driver, config)
        game = GamePage(driver, config)
    }

    override suspend fun beforeEach(testCase: TestCase) {
        check(this::driver.isInitialized) { "Web driver not initialized" }
        driver.manage().deleteAllCookies()
    }

    private fun startClientContainer(spec: Spec): GenericContainer<Nothing> {
        val image = System.getenv(ENV_VAR_CLIENT_IMAGE)?: DEFAULT_CLIENT_IMAGE
        val container = spec.install(TestContainerExtension(image)) {
            withExposedPorts(80)
            withEnv("BASE_PATH", "http://localhost:$serverPort")
            withImagePullPolicy { true }
        }
        return container.also {
            it.start()
        }
    }
}