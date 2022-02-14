package net.bluewizardhat.demoapp.template.service

import mu.KotlinLogging
import net.bluewizardhat.common.cache.RedisCache
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.NewAccountRequest
import net.bluewizardhat.demoapp.template.api.UpdateAccountRequest
import net.bluewizardhat.demoapp.template.database.repository.AccountRepository
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApis
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toEntity
import org.springframework.data.domain.PageRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.transaction.Transactional
import javax.validation.Valid
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

@Validated
@RestController
@RequestMapping("/api/account")
class AccountService(
    private val accountRepository: AccountRepository,
    private val cache: RedisCache
) : AccountOperations {
    private val log = KotlinLogging.logger {}

    @GetMapping(path = ["/"])
    override fun findAllAccounts(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") pageSize: Int
    ): List<Account> {
        log.debug { "findAllAccounts(page = $page, pageSize = $pageSize)" }
        return accountRepository.findAll(PageRequest.of(page, pageSize, AccountEntity.defaultSort)).toApis()
    }

    @GetMapping(path = ["/{id}"])
    override fun getAccountById(@PathVariable("id") id: UUID): Account {
        log.debug { "getAccount('$id')" }
        return cache.cached(id.toString()) {
            accountRepository
                .findById(id)
                .map { it.toApi() }
                .orElseThrow { IllegalArgumentException("Account with id '$id' not found") }
        }
    }

    @PostMapping(path = ["/"])
    override fun saveNewAccount(@Valid @RequestBody request: NewAccountRequest): Account {
        log.debug { "saveNewAccount('$request')" }
        return accountRepository.save(request.toEntity()).toApi()
    }

    @Transactional
    @PatchMapping(path = ["/"])
    override fun updateExistingAccount(@Valid @RequestBody request: UpdateAccountRequest): Int {
        log.debug { "updateExistingAccount('$request')" }
        cache.invalidate(request.id.toString())
        return accountRepository.updateAccount(request.id, request.name!!)
    }
}
