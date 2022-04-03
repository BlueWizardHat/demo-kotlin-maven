package net.bluewizardhat.common.errors

object CommonErrors {
    fun entityNotFound(entityName: String, id: String): DetailedException =
        KeyValueErrorDetails(
            errorCode = "NOT_FOUND",
            message = "$entityName with id '$id' not found",
            details = mapOf(
                Pair(entityName, id)
            )
        ).asException(404)
}
