package de.matthiasfisch.planningpoker.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.matthiasfisch.planningpoker.model.*
import org.jooq.DataType
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType

val lobObjectMapper = jacksonObjectMapper()

class Players(
    alias: String? = null
) {
    val idField = field(alias, "id", SQLDataType.UUID)
    val nameField = field(alias, "name", SQLDataType.VARCHAR)
    val createdAtField = field(alias, "created_at", SQLDataType.TIMESTAMP)
    val updatedAtField = field(alias, "updated_at", SQLDataType.TIMESTAMP)
    val avatarField = field(alias, "avatar", SQLDataType.JSONB.nullable(true))
    val table = DSL.table(DSL.name("players")).let { if (alias != null) it.`as`(alias) else it }

    operator fun invoke(record: Record): Player = with(record) {
        Player(
            get(idField),
            get(nameField),
            get(createdAtField).toInstant(),
            get(updatedAtField).toInstant(),
            get(avatarField)?.let { lobObjectMapper.readValue<Avatar>(it.data()) }
        )
    }
}

class Games(
    alias: String? = null
) {
    val idField = field(alias, "id", SQLDataType.UUID)
    val nameField = field(alias, "name", SQLDataType.VARCHAR)
    val createdAtField = field(alias, "created_at", SQLDataType.TIMESTAMP)
    val updatedAtField = field(alias, "updated_at", SQLDataType.TIMESTAMP)
    val passwordHashField = field(alias, "password_hash", SQLDataType.VARCHAR)
    val playableCardsField = field(alias, "playable_cards", SQLDataType.INTEGER.array())
    val table = DSL.table(DSL.name("games")).let { if (alias != null) it.`as`(alias) else it }

    operator fun invoke(record: Record): Game = with(record) {
        Game(
            get(idField),
            get(nameField),
            get(createdAtField).toInstant(),
            get(updatedAtField).toInstant(),
            get(passwordHashField) != null,
            get(playableCardsField).map { Card(it) }
        )
    }
}

class Rounds(
    alias: String? = null
) {
    val idField = field(alias, "id", SQLDataType.UUID)
    val gameIdField = field(alias, "game_id", SQLDataType.UUID)
    val topicField = field(alias, "topic", SQLDataType.VARCHAR)
    val createdAtField = field(alias, "created_at", SQLDataType.TIMESTAMP)
    val updatedAtField = field(alias, "updated_at", SQLDataType.TIMESTAMP)
    val endedAtField = field(alias, "ended_at", SQLDataType.TIMESTAMP)
    val endedByField = field(alias, "ended_by", SQLDataType.UUID)
    val table = DSL.table(DSL.name("rounds")).let { if (alias != null) it.`as`(alias) else it }

    operator fun invoke(record: Record): Round = with(record) {
        Round(
            get(idField),
            get(gameIdField),
            get(topicField),
            get(createdAtField).toInstant(),
            get(updatedAtField).toInstant(),
            get(endedAtField).toInstant(),
            get(endedByField)
        )
    }
}

class Votes(
    alias: String? = null
) {
    val roundIdField = field(alias, "round_id", SQLDataType.UUID)
    val playerIdField = field(alias, "player_id", SQLDataType.UUID)
    val createdAtField = field(alias, "created_at", SQLDataType.TIMESTAMP)
    val cardField = field(alias, "card", SQLDataType.INTEGER)
    val table = DSL.table(DSL.name("votes")).let { if (alias != null) it.`as`(alias) else it }

    operator fun invoke(record: Record): Vote = with(record) {
        Vote(
            get(playerIdField),
            get(roundIdField),
            get(createdAtField).toInstant(),
            Card(get(cardField))
        )
    }
}

class PlayerGames(
    alias: String? = null
) {
    val playerIdField = field(alias, "player_id", SQLDataType.UUID)
    val gameIdField = field(alias, "game_id", SQLDataType.UUID)
    val joinedAtField = field(alias, "joined_at", SQLDataType.TIMESTAMP)
    val table = DSL.table(DSL.name("player_games")).let { if (alias != null) it.`as`(alias) else it }
}

private fun <T> field(alias: String?, name: String, dataType: DataType<T>): Field<T> {
    val qualifiedName = if (alias != null) {
        DSL.name(alias, name)
    } else {
        DSL.name(name)
    }
    return DSL.field(qualifiedName, dataType)
}