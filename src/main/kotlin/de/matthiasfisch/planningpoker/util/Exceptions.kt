package de.matthiasfisch.planningpoker.util

import org.springframework.http.HttpStatusCode
import org.springframework.web.server.ResponseStatusException

fun notFoundError(message: String? = null) =
    ResponseStatusException(HttpStatusCode.valueOf(404), message)

fun unauthorized(message: String? = null) =
    ResponseStatusException(HttpStatusCode.valueOf(401), message)

fun conflict(message: String? = null) =
    ResponseStatusException(HttpStatusCode.valueOf(409), message)