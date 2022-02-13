package net.bluewizardhat.demoapp.template.api

import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

/**
 * Request to create a new Account.
 */
data class NewAccountRequest(
    @field:NotBlank
    @field:Size(max = Account.nameLength)
    val name: String?
)

/**
 * Request to update an existing Account.
 */
data class UpdateAccountRequest(
    val id: UUID,

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
