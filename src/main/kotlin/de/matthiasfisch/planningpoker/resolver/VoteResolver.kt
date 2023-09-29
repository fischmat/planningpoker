package de.matthiasfisch.planningpoker.resolver

import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.Round
import de.matthiasfisch.planningpoker.model.Vote
import de.matthiasfisch.planningpoker.repository.Players
import de.matthiasfisch.planningpoker.repository.Rounds
import org.jooq.DSLContext
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class VoteResolver(
    private val dslContext: DSLContext
) {

    @SchemaMapping
    fun player(vote: Vote): Player {
        val p = Players()

        return dslContext.select()
            .from(p.table)
            .where(p.idField.eq(vote.playerId))
            .fetchOne()!!
            .let { p(it) }
    }

    @SchemaMapping
    fun round(vote: Vote): Round {
        val r = Rounds()

        return dslContext.select()
            .from(r.table)
            .where(r.idField.eq(vote.roundId))
            .fetchOne()!!
            .let { r(it) }
    }
}