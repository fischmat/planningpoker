package de.matthiasfisch.planningpoker.resolver

import de.matthiasfisch.planningpoker.model.Game
import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.repository.Games
import de.matthiasfisch.planningpoker.repository.PlayerGames
import de.matthiasfisch.planningpoker.repository.Players
import org.jooq.DSLContext
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class PlayerResolver(
    private val dslContext: DSLContext
) {
    @SchemaMapping
    fun games(player: Player): List<Game> {
        val p = Players(alias = "p")
        val g = Games(alias = "g")
        val pg = PlayerGames(alias = "pg")

        return dslContext.select()
            .from(p.table)
            .join(pg.table).on(pg.playerIdField.eq(p.idField))
            .join(g.table).on(g.idField.eq(pg.gameIdField))
            .where(p.idField.eq(player.id))
            .fetch()
            .map { g(it) }
    }
}