package net.bluewizardhat.demoapp.template.service

import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
class AccountService {
    private val log = KotlinLogging.logger {}

    @GetMapping(path = ["/"])
    fun getAccount(): String {
        log.debug { "getAccount()" }
        return "Ok"
    }
}
