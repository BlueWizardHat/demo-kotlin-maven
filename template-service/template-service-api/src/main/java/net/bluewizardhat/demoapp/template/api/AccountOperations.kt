package net.bluewizardhat.demoapp.template.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID

/**
 * Defines the operations to do with Accounts.
 */
interface AccountOperations {
    fun findAccounts(@Min(0) page: Int = 0, @Min(5) @Max(100) pageSize: Int = 10): Page<Account>
    fun getAccountById(id: UUID): Account
    fun saveNewAccount(@Valid request: AccountRequest): Account
    fun updateExistingAccount(id: UUID, @Valid request: AccountRequest)
}
