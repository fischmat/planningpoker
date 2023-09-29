package de.matthiasfisch.planningpoker.resolver

import de.matthiasfisch.planningpoker.model.*
import de.matthiasfisch.planningpoker.repository.Games
import de.matthiasfisch.planningpoker.repository.Players
import de.matthiasfisch.planningpoker.repository.Rounds
import de.matthiasfisch.planningpoker.repository.Votes
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import kotlin.math.pow
import kotlin.math.sqrt

@Controller
class RoundResolver(
    private val dslContext: DSLContext
) {
    @SchemaMapping
    fun game(round: Round): Game {
        val g = Games()

        return dslContext.select()
            .from(g.table)
            .where(g.idField.eq(round.gameId))
            .fetchOne()!!
            .let { g(it) }
    }

    @SchemaMapping
    fun endedBy(round: Round): Player? {
        val p = Players()

        return dslContext.select()
            .from(p.table)
            .where(p.idField.eq(round.endedByPlayerId))
            .fetchOne()
            ?.let { p(it) }
    }

    @SchemaMapping
    fun statistics(round: Round): RoundStatistics? {
        val g = Games(alias = "g")
        val r = Rounds(alias = "r")
        val v = Votes(alias = "v")

        val (votes, game) = dslContext.transactionResult { ctx ->
            val votes = DSL.using(ctx).select()
                .from(v.table)
                .join(r.table).on(v.roundIdField.eq(r.idField))
                .join(g.table).on(r.gameIdField.eq(g.idField))
                .where(r.idField.eq(round.id))
                .fetch()
                .map { v(it) }
            val game = DSL.using(ctx).select()
                .from(g.table)
                .where(g.idField.eq(round.gameId))
                .fetchOne()!!
                .let { g(it) }
            votes to game
        }
        return computeRoundResults(votes, game)
    }

    private fun computeRoundResults(votes: List<Vote>, game: Game): RoundStatistics {
        val minVote = votes.minOfOrNull { it.card.value }
        val maxVote = votes.maxOfOrNull { it.card.value }
        val average = votes.map { it.card.value }.average().takeIf { !it.isNaN() }
        val variance = average?.let { avg ->
            sqrt(votes.map { (it.card.value - avg).pow(2.0) }.average())
        }?.takeIf { !it.isNaN() }
        val suggestedCard = if (average != null) {
            game.playableCards
                .filter { it.value >= average }
                .minByOrNull { it.value }
        } else {
            null
        }

        return RoundStatistics(
            votes = votes,
            minVoteValue = minVote,
            maxVoteValue = maxVote,
            minVotes = votes.filter { it.card.value == minVote },
            maxVotes = votes.filter { it.card.value == maxVote },
            averageVote = average,
            variance = variance,
            suggestedCard = suggestedCard
        )
    }
}