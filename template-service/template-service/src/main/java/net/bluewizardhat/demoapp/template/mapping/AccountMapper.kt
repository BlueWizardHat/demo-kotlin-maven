package net.bluewizardhat.demoapp.template.mapping

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountRequest
import java.time.OffsetDateTime
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

object AccountMapper {
    fun AccountRequest.toEntity(): AccountEntity {
        val now = OffsetDateTime.now()
        return AccountEntity(
            name = name ?: throw IllegalArgumentException("name cannot be null"),
            created = now,
            updated = now
        )
    }

    fun dbToApi(entity: AccountEntity): Account =
        Account(
            id = entity.id ?: throw IllegalArgumentException("id is not supposed to be null"),
            name = entity.name,
            created = entity.created,
            updated = entity.updated
        )

    fun AccountEntity.toApi(): Account = dbToApi(this)
}
