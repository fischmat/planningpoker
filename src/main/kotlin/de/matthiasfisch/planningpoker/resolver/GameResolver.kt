package de.matthiasfisch.planningpoker.resolver

import de.matthiasfisch.planningpoker.model.Game
import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.Round
import de.matthiasfisch.planningpoker.repository.Games
import de.matthiasfisch.planningpoker.repository.PlayerGames
import de.matthiasfisch.planningpoker.repository.Players
import de.matthiasfisch.planningpoker.repository.Rounds
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class GameResolver(
    private val dslContext: DSLContext
) {
    @SchemaMapping
    fun players(game: Game): List<Player> {
        val p = Players(alias = "p")
        val g = Games(alias = "g")
        val pg = PlayerGames(alias = "pg")

        return dslContext.select()
            .from(p.table)
            .join(pg.table).on(pg.playerIdField.eq(p.idField))
            .join(g.table).on(g.idField.eq(pg.gameIdField))
            .where(g.idField.eq(game.id))
            .fetch()
            .map { p(it) }
    }

    @SchemaMapping
    fun rounds(game: Game): List<Round> {
        val r = Rounds()

        return dslContext.select()
            .from(r.table)
            .where(r.gameIdField.eq(game.id))
            .fetch()
            .map { r(it) }
    }

    @SchemaMapping
    fun currentRound(game: Game): Round? {
        val r = Rounds()

        return dslContext.select()
            .from(r.table)
            .where(
                DSL.and(
                    r.gameIdField.eq(game.id),
                    r.endedAtField.isNull
                )
            )
            .fetchOne()
            ?.let { r(it) }
    }
}