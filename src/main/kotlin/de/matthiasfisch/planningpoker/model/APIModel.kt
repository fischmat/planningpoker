package de.matthiasfisch.planningpoker.model

import org.springframework.data.domain.Page

data class GameStub(
    val name: String,
    val password: String?,
    val playableCards: List<Card>
)

data class PlayerStub(
    val name: String
)

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