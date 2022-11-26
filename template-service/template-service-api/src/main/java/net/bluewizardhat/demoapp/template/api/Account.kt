package net.bluewizardhat.demoapp.template.api

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Request to create or update an Account.
 */
data class AccountRequest(
    @field:NotBlank
    @field:Size(max = Account.nameLength)
    val name: String?
)

/**
 * An Account.
 */
data class Account(
    val id: UUID,

    val name: String,

    val created: OffsetDateTime,

    val updated: OffsetDateTime
) {
    companion object {
        const val nameLength: Int = 64
    }
}
