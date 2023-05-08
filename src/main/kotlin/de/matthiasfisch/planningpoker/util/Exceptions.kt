package de.matthiasfisch.planningpoker.util

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

fun notFoundError(message: String? = null) =
    ResponseStatusException(HttpStatusCode.valueOf(404), message)

fun unauthorized(message: String? = null) =
    ResponseStatusException(HttpStatusCode.valueOf(401), message)

fun conflict(message: String? = null) =
    ResponseStatusException(HttpStatusCode.valueOf(409), message)


data class ErrorBody(
    val message: String?
)

@ControllerAdvice
class ExceptionHandler: ResponseEntityExceptionHandler() {

    @ExceptionHandler
    fun handleException(e: IllegalArgumentException): ResponseEntity<ErrorBody> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorBody(e.message))
    }
}