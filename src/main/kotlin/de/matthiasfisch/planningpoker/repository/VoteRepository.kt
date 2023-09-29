package de.matthiasfisch.planningpoker.repository

import de.matthiasfisch.planningpoker.model.Card
import de.matthiasfisch.planningpoker.model.NotFoundException
import de.matthiasfisch.planningpoker.model.Vote
import org.jooq.Configuration
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import java.util.*

class VoteRepository(
    schema: String
) {
    private val roundIdField = DSL.field(DSL.name("round_id"), SQLDataType.UUID)
    private val playerIdField = DSL.field(DSL.name("player_id"), SQLDataType.UUID)
    private val createdAtField = DSL.field(DSL.name("created_at"), SQLDataType.TIMESTAMP)
    private val cardField = DSL.field(DSL.name("card"), SQLDataType.INTEGER)
    private val table = DSL.table(DSL.name(schema, "votes"))

    private fun getVote(roundId: UUID, playerId: UUID, txn: Configuration): Vote {
        return DSL.using(txn).select(roundIdField, playerIdField, createdAtField, cardField)
            .from(table)
            .where(
                DSL.and(
                    roundIdField.eq(roundId),
                    playerIdField.eq(playerId)
                )
            )
            .fetchOne()
            ?.toVote()
            ?: throw NotFoundException("There is no vote by player with ID $playerId in round with ID $roundId.")
    }

    private fun putVote(roundId: UUID, playerId: UUID, card: Int, txn: Configuration): Vote {
        DSL.using(txn).insertInto(table)
            .values(roundIdField, playerIdField, createdAtField, cardField)
            .values(DSL.value(roundId), DSL.value(playerId), DSL.value(card), DSL.now())
            .execute()
        return getVote(roundId, playerId, txn)
    }

    private fun removeVote(roundId: UUID, playerId: UUID, txn: Configuration) {
        DSL.using(txn).deleteFrom(table)
            .where(
                DSL.and(
                    roundIdField.eq(roundId),
                    playerIdField.eq(playerId)
                )
            ).execute()
            .also {
                if (it == 0) {
                    throw NotFoundException("Could not remove vote as there is no vote from player $playerId in round $roundId.")
                }
            }
    }

    private fun Record.toVote() =
        Vote(
            get(roundIdField),
            get(playerIdField),
            get(createdAtField).toInstant(),
            Card(get(cardField))
        )
}