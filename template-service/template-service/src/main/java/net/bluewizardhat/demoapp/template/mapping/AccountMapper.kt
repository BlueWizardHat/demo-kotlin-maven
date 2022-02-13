package net.bluewizardhat.demoapp.template.mapping

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.NewAccountRequest
import java.time.OffsetDateTime
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

object AccountMapper {
    fun NewAccountRequest.toEntity(): AccountEntity {
        val now = OffsetDateTime.now()
        return AccountEntity(
            name = name ?: throw IllegalArgumentException("name cannot be null"),
            created = now,
            updated = now
        )
    }

    fun AccountEntity.toApi(): Account =
        Account(
            id = id ?: throw IllegalArgumentException("id is not supposed to be null"),
            name = name,
            created = created,
            updated = updated
        )

    fun Iterable<AccountEntity>.toApis(): List<Account> =
        this.map { it.toApi() }
}
