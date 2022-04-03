package net.bluewizardhat.common.exception.handling

import mu.KotlinLogging
import net.bluewizardhat.common.errors.ErrorDetails
import net.bluewizardhat.common.errors.KeyValueErrorDetails
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class StandardExceptionsErrorHandlers {
    private val log = KotlinLogging.logger {}

    /**
     * Handles all exceptions not specifically covered elsewhere.
     */
    @ExceptionHandler(Exception::class)
    fun defaultExceptionHandler(exception: Exception): ResponseEntity<ErrorDetails> =
        internalError(HttpStatus.INTERNAL_SERVER_ERROR, exception)

    private fun internalError(httpStatus: HttpStatus, exception: Exception): ResponseEntity<ErrorDetails> {
        val details = KeyValueErrorDetails(
            errorCode = httpStatus.name,
            details = mapOf(
                Pair("exception", exception.javaClass.simpleName)
            )
        )
        if (httpStatus.is5xxServerError) {
            log.error(exception) { "Unhandled ${exception.javaClass.simpleName}: \"${exception.message}\" - Returning [$httpStatus]: $details" }
        } else {
            log.warn { "Unhandled ${exception.javaClass.simpleName}: \"${exception.message}\" - Returning [$httpStatus]: $details" }
        }

        return ResponseEntity.status(httpStatus).body(details)
    }
}
