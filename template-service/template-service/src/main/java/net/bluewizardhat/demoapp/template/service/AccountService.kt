package net.bluewizardhat.demoapp.template.service

import jakarta.transaction.Transactional
import jakarta.validation.Valid
import mu.KotlinLogging
import net.bluewizardhat.common.errors.CommonErrors
import net.bluewizardhat.common.logging.aspect.annotations.LogCall
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.api.Page
import net.bluewizardhat.demoapp.template.database.repository.AccountRepository
import net.bluewizardhat.demoapp.template.mapping.AccountMapper
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toEntity
import net.bluewizardhat.demoapp.template.mapping.PageMapper.toApiPage
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

@Component
class AccountService(
    private val accountRepository: AccountRepository
) : AccountOperations {
    private val log = KotlinLogging.logger {}

    @Transactional
    override fun findAccounts(page: Int, pageSize: Int): Page<Account> {
        return accountRepository
            .findAll(PageRequest.of(page, pageSize, AccountEntity.defaultSort))
            .toApiPage(AccountMapper::dbToApi)
    }

    @Transactional
    override fun getAccountById(id: UUID): Account {
        log.debug { "Fetching account '$id' from database" }
        return accountRepository
            .findById(id)
            .map(AccountMapper::dbToApi)
            .orElseThrow { CommonErrors.entityNotFound("Account", id.toString()) }
    }

    @Transactional
    override fun saveNewAccount(@Valid request: AccountRequest): Account {
        return accountRepository.save(request.toEntity()).toApi()
    }

    @Transactional
    override fun updateExistingAccount(id: UUID, @Valid request: AccountRequest) {
        log.debug { "Updating account '$id'" }
        val account: AccountEntity = accountRepository.findById(id)
            .orElseThrow { CommonErrors.entityNotFound("Account", id.toString()) }
        accountRepository.save(account.copy(name = request.name!!, updated = OffsetDateTime.now()))
    }

    /**
     * Async testing
     */
    @Async
    @LogCall
    @Transactional
    fun getAccountByIdAsync(id: UUID): CompletableFuture<Account> {
        log.debug { "Fetching account '$id' from database" }
        Thread.sleep(500)
        return CompletableFuture.completedFuture(
            accountRepository
                .findById(id)
                .map(AccountMapper::dbToApi)
                .orElseThrow { CommonErrors.entityNotFound("Account", id.toString()) }
        )
    }
}
