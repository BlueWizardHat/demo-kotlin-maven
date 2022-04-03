package net.bluewizardhat.demoapp.template.controller

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import net.bluewizardhat.common.cache.SimpleRedisCacheFactoryWeb
import net.bluewizardhat.common.cache.SimpleRedisCacheWeb.CacheDirectives.MaxAgeExpireAfter
import net.bluewizardhat.common.cache.SimpleRedisCacheWeb.NoCacheDirectives.MaxAge0
import net.bluewizardhat.common.cache.SimpleRedisCacheWeb.NoCacheDirectives.MustRevalidate
import net.bluewizardhat.common.cache.SimpleRedisCacheWeb.NoCacheDirectives.NoCache
import net.bluewizardhat.common.cache.SimpleRedisCacheWeb.NoCacheDirectives.NoStore
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.service.AccountService
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.util.UUID
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@Validated
@RestController
@RequestMapping("/api/account")
class AccountController(
    private val accountService: AccountService,
    private val objectMapper: ObjectMapper,
    private val cacheFactory: SimpleRedisCacheFactoryWeb
) {
    private val log = KotlinLogging.logger {}
    private val accountCache = cacheFactory.forPool("account", objectMapper = objectMapper)

    @GetMapping(path = ["/"])
    fun findAccounts(
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "10") @Min(5) @Max(100) pageSize: Int,
        response: HttpServletResponse
    ): Page<Account> {
        accountCache.cacheControl(response, NoCache, NoStore, MaxAge0, MustRevalidate)
        return accountService.findAccounts(page, pageSize)
    }

    @GetMapping(path = ["/{id}"])
    fun getAccountById(@PathVariable("id") id: UUID, response: HttpServletResponse): Account {
        return accountCache
            .cacheControl(response, MaxAgeExpireAfter)
            .cache(key = id.toString(), expireAfter = Duration.ofHours(1), refreshAfter = Duration.ofMinutes(45)) {
                accountService.getAccountById(id)
            }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = ["/"])
    fun saveNewAccount(@Valid @RequestBody request: AccountRequest): Account {
        return accountService.saveNewAccount(request)
    }

    @Transactional
    @PatchMapping(path = ["/{id}"])
    fun updateExistingAccount(@PathVariable("id") id: UUID, @Valid @RequestBody request: AccountRequest): Int {
        accountCache.invalidate(id.toString())
        return accountService.updateExistingAccount(id, request)
    }

    @Profile("local")
    @PostMapping(path = ["/flushCache"])
    fun flushCache() {
        accountCache.invalidateAll()
    }
}
