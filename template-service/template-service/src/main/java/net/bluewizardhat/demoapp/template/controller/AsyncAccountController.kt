package net.bluewizardhat.demoapp.template.controller

import jakarta.servlet.http.HttpServletResponse
import net.bluewizardhat.common.logging.aspect.annotations.HideValue
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.service.AccountService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Validated
@RestController
@RequestMapping("/api/async/account")
class AsyncAccountController(
    private val accountService: AccountService
) {
    @GetMapping(path = ["/{id}"])
    fun getAccountById(@PathVariable("id") id: UUID, @HideValue response: HttpServletResponse): CompletableFuture<Account> {
        return accountService.getAccountByIdAsync(id)
    }
}
