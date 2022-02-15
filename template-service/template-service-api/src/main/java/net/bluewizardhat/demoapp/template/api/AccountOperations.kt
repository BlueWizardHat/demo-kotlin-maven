package net.bluewizardhat.demoapp.template.api

import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

/**
 * Defines the operation to do on an Account.
 */
interface AccountOperations {
    fun findAllAccounts(@Min(0) page: Int, @Min(5) @Max(100) pageSize: Int): List<Account>
    fun getAccountById(id: UUID): Account
    fun saveNewAccount(@Valid request: NewAccountRequest): Account
    fun updateExistingAccount(@Valid request: UpdateAccountRequest): Int
}
