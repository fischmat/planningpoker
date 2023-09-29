package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.Avatar
import de.matthiasfisch.planningpoker.model.Game
import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.UnauthorizedException
import de.matthiasfisch.planningpoker.repository.*
import de.matthiasfisch.planningpoker.service.PasswordHashingService
import jakarta.servlet.http.HttpSession
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class MutationResolver(
    private val dslContext: DSLContext,
    private val passwordHashingService: PasswordHashingService
) {
    // TODO Unify with QueryController
    private val playerIdSessionAttribute = "playerId"

    @MutationMapping
    fun me(@Argument name: String?, @Argument avatar: Avatar?, session: HttpSession): Player {
        val playerId = session.getPlayerId()

        val p = Players()
        return dslContext.transactionResult { ctx ->
            val avatarLob = avatar?.let { JSONB.valueOf(lobObjectMapper.writeValueAsString(avatar)) }
            DSL.using(ctx).update(p.table)
                .let { if (name != null) it.set(p.nameField, name) else it }
                .let { if (avatarLob != null) it.set(p.avatarField, avatarLob) else it }
                .set(p.updatedAtField, DSL.now())
                .execute()
            DSL.using(ctx).select()
                .from(p.table)
                .where(p.idField.eq(playerId))
                .fetchOne()!!
                .let { p(it) }
        }
    }

    @MutationMapping
    fun game(@Argument name: String, @Argument password: String?, @Argument playableCards: List<Int>): Game {
        val g = Games()

        val gameId = UUID.randomUUID()
        return dslContext.transactionResult { ctx ->
            DSL.using(ctx).insertInto(g.table)
                .columns(g.idField, g.nameField, g.createdAtField, g.updatedAtField, g.passwordHashField, g.playableCardsField)
                .values(
                    DSL.value(gameId),
                    DSL.value(name),
                    DSL.now(),
                    DSL.now(),
                    password?.let { DSL.value(passwordHashingService.encodePlaintext(it)) }?: DSL.field("NULL", SQLDataType.VARCHAR),
                    DSL.array(*playableCards.toTypedArray())
                ).execute()
            DSL.using(ctx).select()
                .from(g.table)
                .where(g.idField.eq(gameId))
                .fetchOne()!!
                .let { g(it) }
        }
    }

    @MutationMapping
    fun newRound(@Argument gameId: UUID, @Argument topic: String?): Game {
        val r = Rounds()
        val g = Games()

        val roundId = UUID.randomUUID()
        return dslContext.transactionResult { ctx ->
            DSL.using(ctx).insertInto(r.table)
                .columns(r.idField, r.gameIdField, r.topicField, r.createdAtField, r.updatedAtField)
                .values(
                    DSL.value(roundId),
                    DSL.value(gameId),
                    topic?.let { DSL.value(it) }?: DSL.field("NULL", SQLDataType.VARCHAR),
                    DSL.now(),
                    DSL.now()
                ).execute()

            DSL.using(ctx).select()
                .from(g.table)
                .where(g.idField.eq(gameId))
                .fetchOne()!!
                .let { g(it) }
        }
    }

    @MutationMapping
    fun endRound(@Argument id: UUID, session: HttpSession): Game {
        val playerId = session.getPlayerId()
        val r = Rounds("r")
        val g = Games("g")

        return dslContext.transactionResult { ctx ->
            DSL.using(ctx).update(r.table)
                .set(r.endedAtField, DSL.now())
                .set(r.endedByField, DSL.value(playerId))

            DSL.using(ctx).select()
                .from(g.table)
                .join(r.table).on(r.gameIdField.eq(g.idField))
                .where(r.idField.eq(id))
                .fetchOne()!!
                .let { g(it) }
        }
    }

    @MutationMapping
    fun joinGame(@Argument id: UUID, session: HttpSession): Game {
        val playerId = session.getPlayerId()
        val g = Games()
        val pg = PlayerGames()

        return dslContext.transactionResult { ctx ->
            DSL.using(ctx).insertInto(pg.table)
                .columns(pg.playerIdField, pg.gameIdField, pg.joinedAtField)
                .values(
                    DSL.value(playerId),
                    DSL.value(id),
                    DSL.now()
                ).execute()
            DSL.using(ctx).select()
                .from(g.table)
                .where(g.idField.eq(id))
                .fetchOne()!!
                .let { g(it) }
        }
    }

    @MutationMapping
    fun newVote(@Argument roundId: UUID, @Argument card: Int, session: HttpSession): Game {
        val playerId = session.getPlayerId()
        val v = Votes()
        val g = Games("g")
        val r = Rounds("r")

        return dslContext.transactionResult { ctx ->
            DSL.using(ctx).insertInto(v.table)
                .columns(v.roundIdField, v.playerIdField, v.createdAtField, v.cardField)
                .values(
                    DSL.value(roundId),
                    DSL.value(playerId),
                    DSL.now(),
                    DSL.value(card)
                ).execute()

            DSL.using(ctx).select()
                .from(r.table)
                .join(g.table).on(g.idField.eq(r.gameIdField))
                .where(r.idField.eq(roundId))
                .fetchOne()!!
                .let { g(it) }
        }
    }

    @MutationMapping
    fun revokeVote(@Argument roundId: UUID, session: HttpSession): Game {
        val playerId = session.getPlayerId()
        val v = Votes()
        val g = Games("g")
        val r = Rounds("r")

        return dslContext.transactionResult { ctx ->
            DSL.using(ctx).deleteFrom(v.table)
                .where(
                    DSL.and(
                        v.playerIdField.eq(playerId),
                        v.roundIdField.eq(roundId)
                    )
                ).execute()

            DSL.using(ctx).select()
                .from(r.table)
                .join(g.table).on(g.idField.eq(r.gameIdField))
                .where(r.idField.eq(roundId))
                .fetchOne()!!
                .let { g(it) }
        }
    }

    private fun HttpSession.getPlayerId() = getAttribute(playerIdSessionAttribute)
        .let { it as? String }
        ?.let { UUID.fromString(it) }
        ?: throw UnauthorizedException("Player does not have a session")
}