package net.bluewizardhat.demoapp.template.api

import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.Size

data class Account(
    val uuid: UUID = UUID.randomUUID(),

    @Size(max = 64)
    val name: String,

    val created: OffsetDateTime = OffsetDateTime.now()
)
