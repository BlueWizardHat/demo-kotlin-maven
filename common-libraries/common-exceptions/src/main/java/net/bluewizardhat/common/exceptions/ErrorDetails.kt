package net.bluewizardhat.common.exceptions

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.OffsetDateTime
import java.time.ZoneOffset

interface ConvertableToErrorDetails {
    fun toErrorDetails(): ErrorDetails
}

private fun nowInUtc(): OffsetDateTime = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "type")
sealed class ErrorDetails : ConvertableToErrorDetails {
    abstract val errorCode: String
    abstract val timestamp: OffsetDateTime
    override fun toErrorDetails() = this
}

/**
 * Error details if there is no content to add.
 */
data class CodeErrorDetails @JsonCreator constructor(
    override val errorCode: String = "UNKNOWN_ERROR",
    override val timestamp: OffsetDateTime = nowInUtc()
) : ErrorDetails()

/**
 * Error details for a single message.
 */
data class MessageErrorDetails @JsonCreator constructor(
    override val errorCode: String,
    val message: String,
    override val timestamp: OffsetDateTime = nowInUtc(),
) : ErrorDetails()

/**
 * Error details with key/values.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class KeyValueErrorDetails @JsonCreator constructor(
    override val errorCode: String,
    val message: String? = null,
    val details: Map<String, String>,
    override val timestamp: OffsetDateTime = nowInUtc()
) : ErrorDetails()

/**
 * Error details type for validation errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ValidationErrorDetails @JsonCreator constructor(
    val message: String? = null,
    val cause: String? = null, // name of the exception that is the cause of the errors
    val errors: List<ValidationError>,
    override val errorCode: String = "VALIDATION_ERRORS",
    override val timestamp: OffsetDateTime = nowInUtc()
) : ErrorDetails() {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ValidationError @JsonCreator constructor(
        val field: String,
        val code: String? = null,
        val type: String? = null,
        val message: String
    )
}

/**
 * Error details for calling another service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CallFailedErrorDetails @JsonCreator constructor(
    val service: String,
    val methodKey: String?,
    val httpStatusCode: Int? = null,
    val httpStatus: String? = null,
    val httpAuthentication: String? = null,
    val details: ErrorDetails
) : ErrorDetails() {
    override val timestamp: OffsetDateTime
        /**
         * Extracts the underlying error code
         */
        @JsonIgnore
        get() = getInnermostCause().timestamp

    override val errorCode: String
        /**
         * Extracts the underlying error code
         */
        @JsonIgnore
        get() = getInnermostCause().errorCode

    /**
     * Extracts the underlying error.
     */
    @JsonIgnore
    fun getInnermostCause(): ErrorDetails {
        var cause = details
        while (cause is CallFailedErrorDetails) {
            cause = cause.details
        }
        return cause
    }
}

/**
 * The error returned when spring returns a 404 status.
 * {"timestamp":"2022-02-13T20:47:22.005+0000","status":404,"error":"Not Found","message":"No message available","path":"/some/path"}
 */
data class SpringNotFoundErrorDetails @JsonCreator constructor(
    val status: Int,
    val error: String,
    val message: String,
    val path: String
) : ConvertableToErrorDetails {
    override fun toErrorDetails() = KeyValueErrorDetails(
        errorCode = "NOT_FOUND",
        timestamp = nowInUtc(),
        details = mapOf(
            Pair("status", status.toString()),
            Pair("error", error),
            Pair("message", message),
            Pair("path", path)
        )
    )
}
