package net.bluewizardhat.demoapp.template.service

import mu.KotlinLogging
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.database.repository.AccountRepository
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApis
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/account")
class AccountService(
    private val accountRepository: AccountRepository
) {
    private val log = KotlinLogging.logger {}

    @GetMapping(path = ["/"])
    fun getAllAccounts(): List<Account> {
        log.debug { "getAllAccounts()" }
        return accountRepository.findAll().toApis()
    }

    @GetMapping(path = ["/{uuid}"])
    fun getAccountByUuid(@PathVariable("uuid") uuid: String): Account {
        log.debug { "getAccount('$uuid')" }
        return accountRepository
            .findByUuid(UUID.fromString(uuid))
            .map { it.toApi() }
            .orElseThrow { IllegalArgumentException("Account with id '$uuid' not found") }
    }
}
