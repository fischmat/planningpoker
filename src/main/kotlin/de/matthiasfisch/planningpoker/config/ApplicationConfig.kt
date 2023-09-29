package de.matthiasfisch.planningpoker.config

import de.matthiasfisch.planningpoker.model.Scalars
import graphql.schema.GraphQLScalarType
import jakarta.servlet.DispatcherType
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.web.filter.ForwardedHeaderFilter
import javax.sql.DataSource


@Configuration
class ApplicationConfig {

    @Bean
    fun forwardedHeaderFilter() = FilterRegistrationBean(ForwardedHeaderFilter()).apply {
        setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR)
        order = Ordered.HIGHEST_PRECEDENCE
    }

    @Bean
    fun flyway(dataSource: DataSource): Flyway {
        return Flyway.configure()
            .dataSource(dataSource)
            .load()
            .also {
                it.migrate()
            }
    }

    @Bean
    fun dslContext(dataSource: DataSource): DSLContext {
        return DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer {
        return RuntimeWiringConfigurer {
            it.scalar(Scalars.timestamp)
        }
    }

    @Bean
    fun timestampScalar(): GraphQLScalarType = Scalars.timestamp
}