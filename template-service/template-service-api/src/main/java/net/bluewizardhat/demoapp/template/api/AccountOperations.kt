package net.bluewizardhat.demoapp.template.api

import org.springframework.data.domain.Page
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

/**
 * Defines the operation to do on an Account.
 */
interface AccountOperations {
    fun findAllAccounts(@Min(0) page: Int, @Min(5) @Max(100) pageSize: Int): Page<Account>
    fun getAccountById(id: UUID): Account
    fun saveNewAccount(@Valid request: AccountRequest): Account
    fun updateExistingAccount(id: UUID, @Valid request: AccountRequest): Int
}
