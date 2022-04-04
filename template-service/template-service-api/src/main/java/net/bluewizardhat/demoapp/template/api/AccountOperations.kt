package net.bluewizardhat.demoapp.template.api

import org.springframework.data.domain.Page
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

/**
 * Defines the operations to do with Accounts.
 */
interface AccountOperations {
    fun findAccounts(@Min(0) page: Int = 0, @Min(5) @Max(100) pageSize: Int = 10): Page<Account>
    fun getAccountById(id: UUID): Account
    fun saveNewAccount(@Valid request: AccountRequest): Account
    fun updateExistingAccount(id: UUID, @Valid request: AccountRequest): Int
}
