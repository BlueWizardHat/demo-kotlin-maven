package net.bluewizardhat.demoapp.template.client

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.AccountRequest
import org.springframework.data.domain.Page
import java.util.UUID

class AccountClient : AccountOperations {
    override fun findAllAccounts(page: Int, pageSize: Int): Page<Account> {
        TODO("Not yet implemented")
    }

    override fun getAccountById(id: UUID): Account {
        TODO("Not yet implemented")
    }

    override fun saveNewAccount(request: AccountRequest): Account {
        TODO("Not yet implemented")
    }

    override fun updateExistingAccount(id: UUID, request: AccountRequest): Int {
        TODO("Not yet implemented")
    }
}
