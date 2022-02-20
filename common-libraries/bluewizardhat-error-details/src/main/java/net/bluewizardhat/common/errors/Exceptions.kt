package net.bluewizardhat.common.errors

sealed class DetailedException(
    message: String?,
    val details: ErrorDetails,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class OtherException
