package net.bluewizardhat.demoapp.template.client

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Client
import feign.Feign
import feign.Headers
import feign.Request
import feign.RequestLine
import feign.Retryer
import feign.jackson.JacksonEncoder
import mu.KotlinLogging
import net.bluewizardhat.common.logging.aspect.LogInvocation
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.AccountRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Client for calling the AccountController from another service.
 */
@Component
@LogInvocation
class AccountFeignClient(
    @Value("\${template-service.endpoints.accounts}")
    private val endpoint: String,
    objectMapper: ObjectMapper,
    client: Client
) : AccountOperations {
    companion object {
        const val urlPrefix = "/api//account"
        const val connectTimeoutSeconds: Long = 10
        const val readTimeoutSeconds: Long = 60

        interface AccountServiceFeignApi : AccountOperations {
            @RequestLine("GET $urlPrefix/")
            @Headers("Content-Type: application/json")
            override fun findAccounts(page: Int, pageSize: Int): Page<Account>

            @RequestLine("GET $urlPrefix/{id}")
            @Headers("Content-Type: application/json")
            override fun getAccountById(id: UUID): Account

            @RequestLine("GET $urlPrefix/")
            @Headers("Content-Type: application/json")
            override fun saveNewAccount(request: AccountRequest): Account

            @RequestLine("PATCH $urlPrefix/{id}")
            @Headers("Content-Type: application/json")
            override fun updateExistingAccount(id: UUID, request: AccountRequest): Int
        }
    }

    private val log = KotlinLogging.logger {}

    private val feignClient: AccountServiceFeignApi = Feign.builder()
        .client(client)
        .encoder(JacksonEncoder(objectMapper))
        .retryer(Retryer.NEVER_RETRY)
        .options(Request.Options(connectTimeoutSeconds, TimeUnit.SECONDS, readTimeoutSeconds, TimeUnit.SECONDS, true))
        .target(AccountServiceFeignApi::class.java, endpoint)

    init {
        log.info { "Initialized with endpoint $endpoint" }
    }

    override fun findAccounts(page: Int, pageSize: Int): Page<Account> =
        feignClient.findAccounts(page, pageSize)

    override fun getAccountById(id: UUID): Account =
        feignClient.getAccountById(id)

    override fun saveNewAccount(request: AccountRequest): Account =
        feignClient.saveNewAccount(request)

    override fun updateExistingAccount(id: UUID, request: AccountRequest): Int =
        feignClient.updateExistingAccount(id, request)
}
