package net.bluewizardhat.demoapp.template.service

import mu.KotlinLogging
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.NewAccountRequest
import net.bluewizardhat.demoapp.template.api.UpdateAccountRequest
import net.bluewizardhat.demoapp.template.database.repository.AccountRepository
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApis
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.transaction.Transactional
import javax.validation.Valid

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
    fun getAccountByUuid(@PathVariable("uuid") uuid: UUID): Account {
        log.debug { "getAccount('$uuid')" }
        return accountRepository
            .findByUuid(uuid)
            .map { it.toApi() }
            .orElseThrow { IllegalArgumentException("Account with id '$uuid' not found") }
    }

    @PostMapping(path = ["/"])
    fun saveNewAccount(@Valid @RequestBody request: NewAccountRequest): Account {
        log.debug { "saveNewAccount('$request')" }
        return accountRepository.save(request.toEntity()).toApi()
    }

    @Transactional
    @PatchMapping(path = ["/"])
    fun updateExistingAccount(@Valid @RequestBody request: UpdateAccountRequest): Int {
        log.debug { "updateExistingAccount('$request')" }
        return accountRepository.updateAccount(request.uuid, request.name)
    }
}
