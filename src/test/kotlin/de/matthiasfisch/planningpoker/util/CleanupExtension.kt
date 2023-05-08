package de.matthiasfisch.planningpoker.util

import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import mu.KotlinLogging

private val LOGGER = KotlinLogging.logger {}

class CleanupExtension: AfterEachListener {
    private val actions = mutableSetOf<() -> Unit>()

    fun addTask(action: () -> Unit) = synchronized(actions) {
        actions.add(action)
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        actions.forEach { action ->
            runCatching(action).onFailure { e ->
                LOGGER.error(e) { "Failed to perform cleanup action" }
            }
        }
    }
}