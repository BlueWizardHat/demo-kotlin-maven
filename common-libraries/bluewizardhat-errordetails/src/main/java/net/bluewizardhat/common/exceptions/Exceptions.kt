package net.bluewizardhat.common.exceptions

sealed class ErrorDetailsException(
    message: String?,
    val details: ErrorDetails,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class OtherException
