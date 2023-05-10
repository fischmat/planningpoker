package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.PlayerStub
import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.mapper.ObjectMapperType

object Players {

    fun createPlayerSession(): PlayerSession =
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(PlayerStub("it-player"), ObjectMapperType.JACKSON_2)
            .`when`()
            .request(Method.POST, "/v1/players")
            .then()
            .extract()
            .let {
                PlayerSession(
                    player = it.`as`(Player::class.java),
                    sessionId = it.cookie(Api.sessionCookieName)
                )
            }

    fun getPlayerResponse(id: String) =
        RestAssured.given()
            .`when`()
            .request(Method.GET, "/v1/players/{playerId}", id)

    fun getPlayer(id: String) =
        getPlayerResponse(id)
            .then()
            .statusCode(200)
            .extract()
            .`as`(Player::class.java)
}

data class PlayerSession(
    val player: Player,
    val sessionId: String
)