package net.bluewizardhat.demoapp.template.mapping

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

object AccountMapper {
    fun Account.toEntity(): AccountEntity =
        AccountEntity(
            uuid = uuid,
            name = name,
            created = created
        )

    fun Iterable<Account>.toEntities(): List<AccountEntity> =
        this.map { it.toEntity() }

    fun AccountEntity.toApi(): Account =
        Account(
            uuid = uuid,
            name = name,
            created = created
        )

    fun Iterable<AccountEntity>.toApis(): List<Account> =
        this.map { it.toApi() }
}
