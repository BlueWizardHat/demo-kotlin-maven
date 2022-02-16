package net.bluewizardhat.demoapp.template.api

import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

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
