package net.bluewizardhat.demoapp.template.api

import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.Size

data class NewAccountRequest(
    @Size(max = Account.nameLength)
    val name: String
)

data class UpdateAccountRequest(
    val uuid: UUID = UUID.randomUUID(),

    @Size(max = Account.nameLength)
    val name: String
)

data class Account(
    val uuid: UUID = UUID.randomUUID(),

    @Size(max = nameLength)
    val name: String,

    val created: OffsetDateTime,

    val updated: OffsetDateTime
) {
    companion object {
        const val nameLength = 64
    }
}
