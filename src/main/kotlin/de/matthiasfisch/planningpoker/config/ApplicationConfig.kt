package de.matthiasfisch.planningpoker.config

import jakarta.servlet.DispatcherType
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.session.data.mongo.JdkMongoSessionConverter
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import org.springframework.web.filter.ForwardedHeaderFilter
import java.time.Duration


@Configuration
@EnableMongoHttpSession
class ApplicationConfig {

    @Bean
    fun forwardedHeaderFilter() = FilterRegistrationBean(ForwardedHeaderFilter()).apply {
        setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR)
        order = Ordered.HIGHEST_PRECEDENCE
    }

    @Bean
    fun jdkMongoSessionConverter(): JdkMongoSessionConverter? {
        return JdkMongoSessionConverter(Duration.ofMinutes(30))
    }
}