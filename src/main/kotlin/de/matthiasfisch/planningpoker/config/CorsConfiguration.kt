package de.matthiasfisch.planningpoker.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
@Profile("dev", "e2e")
class CorsConfiguration: WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .exposedHeaders("*")
            .allowedMethods("*")
            .allowedOriginPatterns("http://localhost:[*]")
            .allowCredentials(true)
    }
}