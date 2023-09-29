package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.Game
import de.matthiasfisch.planningpoker.model.NotFoundException
import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.repository.Games
import de.matthiasfisch.planningpoker.repository.Players
import jakarta.servlet.http.HttpSession
import org.jooq.DSLContext
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class QueryController(
    private val dslContext: DSLContext
) {
    private val playerIdSessionAttribute = "playerId"

    @QueryMapping
    fun me(session: HttpSession): Player? {
        val playerId = session.getAttribute(playerIdSessionAttribute)
            .let { it as? String }
            ?.let { UUID.fromString(it) }
            ?: return null

        val p = Players()
        return dslContext.select()
            .from(p.table)
            .where(p.idField.eq(playerId))
            .fetchOne()
            ?.let { p(it) }
    }

    @QueryMapping
    fun player(@Argument id: UUID): Player {
        val p = Players()
        return dslContext.select()
            .from(p.table)
            .where(p.idField.eq(id))
            .fetchOne()
            ?.let { p(it) }
            ?: throw NotFoundException("Player with ID $id does not exist.")
    }

    @QueryMapping
    fun game(@Argument id: UUID): Game? {
        val g = Games()
        return dslContext.select()
            .from(g.table)
            .where(g.idField.eq(id))
            .fetchOne()
            ?.let { g(it) }
    }
}