package de.matthiasfisch.planningpoker.model

import org.springframework.data.domain.Page

data class GameStub(
    val name: String,
    val password: String?,
    val playableCards: List<Card>
)

data class PlayerStub(
    val name: String,
    val avatar: AvatarProps? = null
)

data class AvatarProps(
    val backgroundColor: String,
    val earrings: Int?,
    val eyebrows: Int,
    val eyes: Int,
    val features: List<String>,
    val glasses: Int?,
    val hair: Int?,
    val longHair: Boolean,
    val hairColor: String,
    val mouth: Int,
    val skinColor: String
) {
    init {
        require(backgroundColor == "transparent" || backgroundColor.matches("[a-fA-F0-9]{6}".toRegex())) { "backgroundColor must match [a-fA-F0-9]{6}" }
        require(earrings == null || earrings in 1..6) { "earrings out of range." }
        require(eyebrows in 1..15) { "eyebrows out of range." }
        require(eyes in 1..26) { "eyes out of range." }
        require(glasses == null || glasses in 1..5) { "glasses out of range." }
        if (longHair) {
            require(hair == null || hair in 1..26) { "hair out of range." }
        } else {
            require(hair == null || hair in 1..19) { "hair out of range." }
        }
        require(mouth in 1..30) { "mouth out of range." }
        require(hairColor.matches("[a-fA-F0-9]{6}".toRegex())) { "hairColor must match [a-fA-F0-9]{6}" }
        require(skinColor.matches("[a-fA-F0-9]{6}".toRegex())) { "skinColor must match [a-fA-F0-9]{6}" }
    }
}

data class RoundStub(
    val topic: String
)

data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
) {
    constructor(page: Page<T>): this(
        page.content,
        page.pageable.pageNumber,
        page.pageable.pageSize,
        page.totalPages
    )
}

// Info

data class ApplicationInfo(
    val socketIO: SocketIOInfo
)

data class SocketIOInfo(
    val host: String,
    val port: Int
)