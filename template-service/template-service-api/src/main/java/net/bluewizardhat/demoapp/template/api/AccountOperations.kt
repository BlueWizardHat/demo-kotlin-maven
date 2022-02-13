package net.bluewizardhat.demoapp.template.api

import java.util.UUID
import javax.validation.Valid

interface AccountOperations {
    fun findAllAccounts(page: Int, pageSize: Int): List<Account>
    fun getAccountById(id: UUID): Account
    fun saveNewAccount(@Valid request: NewAccountRequest): Account
    fun updateExistingAccount(@Valid request: UpdateAccountRequest): Int
}
