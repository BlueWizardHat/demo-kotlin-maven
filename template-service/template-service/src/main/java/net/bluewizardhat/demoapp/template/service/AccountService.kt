package net.bluewizardhat.demoapp.template.service

import mu.KotlinLogging
import net.bluewizardhat.common.errors.CommonErrors
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.database.repository.AccountRepository
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApis
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.util.UUID
import javax.validation.Valid

@Component
class AccountService(
    private val accountRepository: AccountRepository,
) : AccountOperations {
    private val log = KotlinLogging.logger {}

    override fun findAccounts(page: Int, pageSize: Int): Page<Account> {
        return accountRepository.findAll(PageRequest.of(page, pageSize, net.bluewizardhat.demoapp.template.database.entity.Account.defaultSort)).toApis()
    }

    override fun getAccountById(id: UUID): Account {
        log.debug { "Fetching account '$id' from database" }
        return accountRepository
            .findById(id)
            .map { it.toApi() }
            .orElseThrow { CommonErrors.entityNotFound("Account", id.toString()) }
    }

    override fun saveNewAccount(@Valid request: AccountRequest): Account {
        return accountRepository.save(request.toEntity()).toApi()
    }

    override fun updateExistingAccount(id: UUID, @Valid request: AccountRequest): Int {
        return accountRepository.updateAccount(id, request.name!!)
    }
}
