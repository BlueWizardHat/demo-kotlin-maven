package net.bluewizardhat.common.errors

class DetailedException(
    message: String?,
    val httpStatusCode: Int,
    val details: ErrorDetails,
    cause: Throwable? = null
) : RuntimeException(message, cause)
