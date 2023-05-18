package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.GameRepository
import de.matthiasfisch.planningpoker.service.PasswordHashingService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.lang.IllegalStateException

@Component
class GameAuthFilter(
    private val gameRepository: GameRepository,
    private val passwordHashing: PasswordHashingService,
    @Value("\${security.enforce-https.password-transfer}") private val enforceHttps: Boolean
) : OncePerRequestFilter() {
    private val protectedPathRegex = ".*/api/v[0-9]+/games/(.*?)/players$".toRegex()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Only allow via HTTPS
        if (enforceHttps && request.scheme.lowercase() != "https") {
            response.status = 426
            response.addHeader(HttpHeaders.UPGRADE, "HTTPS")
            return
        }

        val game = getGameFromPath(request.servletPath).orElse(null)
        val suppliedHash = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()

        if (game?.passwordHash != null && (suppliedHash == null || !passwordHashing.intermediateMatches(suppliedHash, game.passwordHash))) {
            response.status = 401
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        } else {
            filterChain.doFilter(request, response)
        }
    }

    private fun getGameFromPath(path: String) =
        protectedPathRegex.find(path)
            ?.groups
            ?.get(1)
            ?.value
            ?.let { gameRepository.findById(it) }
            ?: throw IllegalStateException("Authenticated required unexpectedly at path ${path}.")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.method != "POST" || !request.servletPath.matches(protectedPathRegex)
    }
}