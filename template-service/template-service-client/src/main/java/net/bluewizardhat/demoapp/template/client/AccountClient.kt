package net.bluewizardhat.demoapp.template.client

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.NewAccountRequest
import net.bluewizardhat.demoapp.template.api.UpdateAccountRequest
import java.util.UUID

class AccountClient : AccountOperations {
    override fun findAllAccounts(page: Int, pageSize: Int): List<Account> {
        TODO("Not yet implemented")
    }

    override fun getAccountById(id: UUID): Account {
        TODO("Not yet implemented")
    }

    override fun saveNewAccount(request: NewAccountRequest): Account {
        TODO("Not yet implemented")
    }

    override fun updateExistingAccount(request: UpdateAccountRequest): Int {
        TODO("Not yet implemented")
    }
}
