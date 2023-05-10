@file:Suppress("UNCHECKED_CAST")

package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.*
import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.mapper.ObjectMapperType
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import org.springframework.http.HttpHeaders

object Games {

    fun getPage(page: Int, limit: Int): PagedResult<Map<String, Any>> =
        getPageResponse(page, limit)
            .then()
            .statusCode(200)
            .extract()
            .`as`(PagedResult::class.java) as PagedResult<Map<String, Any>>

    fun getPageResponse(page: Int, limit: Int): Response =
        RestAssured.given()
            .queryParam("page", page)
            .queryParam("limit", limit)
            .`when`()
            .request(Method.GET, "/v1/games")

    fun getGameResponse(gameId: String): Response =
        RestAssured.`when`()
            .request(Method.GET, "/v1/games/{gameId}", gameId)

    fun getGame(gameId: String): Game =
        getGameResponse(gameId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Game::class.java)

    fun createGameResponse(stub: GameStub, sessionId: String?): Response =
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(stub, ObjectMapperType.JACKSON_2)
            .addSessionId(sessionId)
            .`when`()
            .request(Method.POST, "/v1/games")

    fun createGame(stub: GameStub, sessionId: String?): Game =
        createGameResponse(stub, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Game::class.java)

    fun joinGameResponse(gameId: String, sessionId: String?, bearerToken: String?): Response =
        RestAssured.given()
            .addSessionId(sessionId)
            .addBearerToken(bearerToken)
            .`when`()
            .request(Method.POST, "/v1/games/{gameId}/players", gameId)

    fun joinGame(gameId: String, sessionId: String?, bearerToken: String?): Player =
        joinGameResponse(gameId, sessionId, bearerToken)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Player::class.java)

    fun leaveGameResponse(gameId: String, sessionId: String?): Response =
        RestAssured.given()
            .addSessionId(sessionId)
            .`when`()
            .request(Method.DELETE, "/v1/games/{gameId}/players", gameId)

    fun leaveGame(gameId: String, sessionId: String?, bearerToken: String?): Player =
        leaveGameResponse(gameId, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Player::class.java)

    fun getRoundsResponse(gameId: String, sessionId: String?): Response =
        RestAssured.given()
            .addSessionId(sessionId)
            .`when`()
            .request(Method.GET, "/v1/games/{gameId}/rounds", gameId)

    fun getRounds(gameId: String, sessionId: String?): List<Map<String, String>> =
        getRoundsResponse(gameId, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(List::class.java) as List<Map<String, String>>

    fun startRoundResponse(gameId: String, stub: RoundStub, sessionId: String?): Response =
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(stub, ObjectMapperType.JACKSON_2)
            .addSessionId(sessionId)
            .`when`()
            .request(Method.POST, "/v1/games/{gameId}/rounds", gameId)

    fun startRound(gameId: String, stub: RoundStub, sessionId: String?): Round =
        startRoundResponse(gameId, stub, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Round::class.java)

    fun endRoundResponse(gameId: String, roundId: String, sessionId: String?) =
        RestAssured.given()
            .addSessionId(sessionId)
            .`when`()
            .request(Method.DELETE, "/v1/games/{gameId}/rounds/{roundId}", gameId, roundId)

    fun endRound(gameId: String, roundId: String, sessionId: String?) =
        endRoundResponse(gameId, roundId, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Round::class.java)

    private fun RequestSpecification.addSessionId(sessionId: String?) =
        if (sessionId != null) {
            cookie(Api.sessionCookieName, sessionId)
        } else {
            this
        }

    private fun RequestSpecification.addBearerToken(bearerToken: String?) =
        if (bearerToken != null) {
            header(HttpHeaders.AUTHORIZATION, "Bearer $bearerToken")
        } else {
            this
        }
}