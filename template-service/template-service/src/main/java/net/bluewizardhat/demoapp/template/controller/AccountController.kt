package net.bluewizardhat.demoapp.template.controller

import mu.KotlinLogging
import net.bluewizardhat.common.cache.SimpleRedisCacheFactoryWeb
import net.bluewizardhat.common.cache.SimpleRedisCacheWeb.CACHE_DIRECTIVES.maxAge
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.database.repository.AccountRepository
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApis
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toEntity
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Page
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
import java.time.Duration
import java.util.UUID
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

@Validated
@RestController
@RequestMapping("/api/account")
class AccountController(
    private val accountRepository: AccountRepository,
    private val cacheFactory: SimpleRedisCacheFactoryWeb
) {
    private val log = KotlinLogging.logger {}
    private val accountCache = cacheFactory.forPool("account")

    @GetMapping(path = ["/"])
    fun findAllAccounts(
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "10") @Min(5) @Max(100) pageSize: Int
    ): Page<Account> {
        return accountRepository.findAll(PageRequest.of(page, pageSize, AccountEntity.defaultSort)).toApis()
    }

    @GetMapping(path = ["/{id}"])
    fun getAccountById(@PathVariable("id") id: UUID, response: HttpServletResponse): Account {
        return accountCache
            .cacheControl(response, maxAge)
            .cache(key = id.toString(), expireAfter = Duration.ofHours(1), refreshAfter = Duration.ofMinutes(45)) {
                log.debug { "Fetching account '$id' from database" }
                accountRepository
                    .findById(id)
                    .map { it.toApi() }
                    .orElseThrow { IllegalArgumentException("Account with id '$id' not found") }
            }
    }

    @PostMapping(path = ["/"])
    fun saveNewAccount(@Valid @RequestBody request: AccountRequest): Account {
        return accountRepository.save(request.toEntity()).toApi()
    }

    @Transactional
    @PatchMapping(path = ["/{id}"])
    fun updateExistingAccount(@PathVariable("id") id: UUID, @Valid @RequestBody request: AccountRequest): Int {
        accountCache.invalidate(id.toString())
        return accountRepository.updateAccount(id, request.name!!)
    }

    @Profile("local")
    @PostMapping(path = ["/flushCache"])
    fun flushCache() {
        accountCache.invalidateAll()
    }
}
