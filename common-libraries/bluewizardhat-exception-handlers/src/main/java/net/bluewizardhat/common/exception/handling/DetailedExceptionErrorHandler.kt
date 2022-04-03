package net.bluewizardhat.common.exception.handling

import mu.KotlinLogging
import net.bluewizardhat.common.errors.DetailedException
import net.bluewizardhat.common.errors.ErrorDetails
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class DetailedExceptionErrorHandler {
    private val log = KotlinLogging.logger {}

    @ExceptionHandler(DetailedException::class)
    fun handleDetailedException(exception: DetailedException): ResponseEntity<ErrorDetails> {
        val details = exception.details
        val httpStatus: String = HttpStatus.resolve(exception.httpStatusCode)?.toString() ?: "${exception.httpStatusCode} N/A"
        if (exception.httpStatusCode >= 500) {
            log.error(exception) { "${exception.javaClass.simpleName}: \"${exception.message}\" - Returning [$httpStatus]: $details" }
        } else {
            log.warn { "${exception.javaClass.simpleName}: \"${exception.message}\" - Returning [$httpStatus]: $details" }
        }
        return ResponseEntity.status(exception.httpStatusCode).body(details)
    }
}
