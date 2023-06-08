package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.AvatarProps
import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.PlayerStub
import io.restassured.RestAssured
import io.restassured.http.Method
import io.restassured.mapper.ObjectMapperType

object Players {

    fun createPlayerSession(avatar: AvatarProps? = null): PlayerSession =
        RestAssured.given()
            .header("Content-Type", "application/json")
            .body(PlayerStub("it-player", avatar), ObjectMapperType.JACKSON_2)
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

    fun getAvatarResponse(id: String) =
        RestAssured.given()
            .`when`()
            .request(Method.GET, "/v1/players/{playerId}/avatar", id)

    fun getAvatarPreview(
        backgroundColor: String,
        earrings: Int?,
        eyebrows: Int,
        eyes: Int,
        features: List<String>,
        glasses: Int?,
        hair: Int?,
        longHair: Boolean,
        hairColor: String,
        mouth: Int,
        skinColor: String
    ) =
        RestAssured.given()
            .header("Content-Type", "application/json")
            .queryParams(
                mapOf(
                    "backgroundColor" to backgroundColor,
                    "earrings" to earrings,
                    "eyebrows" to eyebrows,
                    "eyes" to eyes,
                    "features" to features,
                    "glasses" to glasses,
                    "hair" to hair,
                    "longHair" to longHair,
                    "hairColor" to hairColor,
                    "mouth" to mouth,
                    "skinColor" to skinColor
                )
            )
            .`when`()
            .request(Method.GET, "/v1/players/avatar/preview")
}

data class PlayerSession(
    val player: Player,
    val sessionId: String
)