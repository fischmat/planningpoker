package de.matthiasfisch.planningpoker.integration

import de.matthiasfisch.planningpoker.controller.Api
import de.matthiasfisch.planningpoker.controller.Players
import de.matthiasfisch.planningpoker.model.AvatarProps
import de.matthiasfisch.planningpoker.util.RestAssuredExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.hamcrest.Matchers.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class AvatarIT(
    @LocalServerPort val serverPort: Int
) : FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension, RestAssuredExtension(serverPort))

    val avatarProps = AvatarProps(
        backgroundColor = "transparent",
        earrings = 1,
        eyebrows = 2,
        eyes = 3,
        glasses = null,
        longHair = false,
        hair = 15,
        hairColor = "aabbcc",
        mouth = 20,
        skinColor = "ffdbac",
        features = listOf()
    )

    init {
        context("GET /v1/players/{playerId}/avatar") {
            val (player, _) = Players.createPlayerSession(avatarProps)

            test("Get avatar of existing player -> 200") {
                // Arrange

                // Act
                val response = Api.players.getAvatarResponse(player.id!!)

                // Assert
                response.then()
                    .statusCode(200)
                    .contentType("image/svg+xml")
            }

            test("Get avatar of non-existing player -> 404") {
                // Arrange

                // Act + Assert
                Api.players.getAvatarResponse("non-existing")
                    .then()
                    .statusCode(404)
                    .contentType(not(equalTo("image/svg+xml")))
                    .body(containsString("Player with ID non-existing does not exist"))
            }
        }

        context("GET /v1/players/avatar/preview") {
            test("Get preview for valid parameters -> 200") {
                // Arrange

                // Act + Assert
                with(avatarProps) {
                    Api.players.getAvatarPreview(
                        backgroundColor,
                        earrings,
                        eyebrows,
                        eyes,
                        features,
                        glasses,
                        hair,
                        longHair,
                        hairColor,
                        mouth,
                        skinColor
                    )
                }
                    .then()
                    .statusCode(200)
                    .contentType("image/svg+xml")
            }

            test("Get preview for invalid parameters -> 400") {
                // Arrange

                // Act + Assert
                with(avatarProps) {
                    Api.players.getAvatarPreview(
                        backgroundColor,
                        earrings,
                        eyebrows,
                        9999, // Invalid value
                        features,
                        glasses,
                        hair,
                        longHair,
                        hairColor,
                        mouth,
                        skinColor
                    )
                }
                    .then()
                    .statusCode(400)
                    .contentType(not(equalTo("image/svg+xml")))
                    .body(containsString("eyes out of range."))
            }
        }
    }
}