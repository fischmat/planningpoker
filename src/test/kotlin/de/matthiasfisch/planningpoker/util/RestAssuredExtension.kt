package de.matthiasfisch.planningpoker.util

import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.restassured.RestAssured

class RestAssuredExtension(val serverPort: Int): BeforeSpecListener {
    override suspend fun beforeSpec(spec: Spec) {
        RestAssured.baseURI = "http://localhost"
        RestAssured.basePath = "/api"
        RestAssured.port = serverPort
    }
}