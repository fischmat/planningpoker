@file:Suppress("UNCHECKED_CAST")

package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.Game
import de.matthiasfisch.planningpoker.model.GameStub
import de.matthiasfisch.planningpoker.model.PagedResult
import de.matthiasfisch.planningpoker.model.RoundStub
import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.mapper.ObjectMapperType
import io.restassured.response.Response

object Games {

    fun getPage(page: Int, limit: Int): PagedResult<Map<String, String>> =
        getPageResponse(page, limit)
            .then()
            .statusCode(200)
            .extract()
            .`as`(PagedResult::class.java) as PagedResult<Map<String, String>>

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
            .let {
                if (sessionId != null) {
                    it.cookie(Api.sessionCookieName, sessionId)
                } else {
                    it
                }
            }
            .`when`()
            .request(Method.POST, "/v1/games")

    fun createGame(stub: GameStub, sessionId: String?): Game =
        createGameResponse(stub, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Game::class.java)

    fun startRoundResponse(gameId: String, stub: RoundStub, sessionId: String?): Response =
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(stub, ObjectMapperType.JACKSON_2)
            .let {
                if (sessionId != null) {
                    it.cookie(Api.sessionCookieName, sessionId)
                } else {
                    it
                }
            }
            .`when`()
            .request(Method.POST, "/v1/games/{gameId}/rounds", gameId)

    fun startRound(gameId: String, stub: RoundStub, sessionId: String?): List<Map<String, String>> =
        startRoundResponse(gameId, stub, sessionId)
            .then()
            .statusCode(200)
            .extract()
            .`as`(List::class.java) as List<Map<String, String>>
}