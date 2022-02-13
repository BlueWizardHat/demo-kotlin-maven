package net.bluewizardhat.demoapp.template.mapping

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.NewAccountRequest
import java.time.OffsetDateTime
import java.util.UUID
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

object AccountMapper {
    fun NewAccountRequest.toEntity(): AccountEntity {
        val now = OffsetDateTime.now()
        return AccountEntity(
            uuid = UUID.randomUUID(),
            name = name,
            created = now,
            updated = now
        )
    }

    fun AccountEntity.toApi(): Account =
        Account(
            uuid = uuid,
            name = name,
            created = created,
            updated = updated
        )

    fun Iterable<AccountEntity>.toApis(): List<Account> =
        this.map { it.toApi() }
}
