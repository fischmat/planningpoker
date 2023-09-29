package de.matthiasfisch.planningpoker.model

import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.*
import java.time.Instant
import java.util.*

object Scalars {
    val timestamp = GraphQLScalarType.newScalar()
        .name("Timestamp")
        .description("An ISO timestamp")
        .coercing(TimestampCoercing)
        .build()
}

private object TimestampCoercing: Coercing<Instant, String> {
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): String =
        (dataFetcherResult as? Instant)?.toString()
            ?: throw CoercingSerializeException("Expected ${Instant::class.java}, but got '$dataFetcherResult' of type ${dataFetcherResult.javaClass}.")

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Instant =
        (input as? CharSequence)
            ?.let {
                kotlin.runCatching { Instant.parse(it) }
                    .onFailure { throw CoercingParseValueException(it) }
                    .getOrNull()
            }
            ?: throw CoercingParseValueException("Expected ${String::class.java}, but got $input of type ${input.javaClass}.")

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): Instant =
        (input as? StringValue)
            ?.let {
                kotlin.runCatching { Instant.parse(it.value) }
                    .onFailure { throw CoercingParseValueException(it) }
                    .getOrNull()
            }
            ?: throw CoercingParseLiteralException("Expected ${String::class.java}, but got $input of type ${input.javaClass}.")
}