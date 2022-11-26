package net.bluewizardhat.demoapp.template.client

import com.fasterxml.jackson.databind.ObjectMapper
import feign.Client
import feign.Feign
import feign.Headers
import feign.Param
import feign.Request
import feign.RequestLine
import feign.Retryer
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import mu.KotlinLogging
import net.bluewizardhat.common.logging.aspect.annotations.LogCall
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.api.Page
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Client for calling the AccountController from another service.
 */
@Component
@LogCall
class AccountFeignClient(
    @Value("\${template-service.endpoints.accounts}")
    private val endpoint: String,
    objectMapper: ObjectMapper,
    client: Client
) : AccountOperations {
    companion object {
        const val urlPrefix = "/api/account"
        const val connectTimeoutSeconds: Long = 10
        const val readTimeoutSeconds: Long = 60

        interface AccountServiceFeignApi : AccountOperations {
            @RequestLine("GET $urlPrefix/?page={page}&pageSize={pageSize}")
            @Headers("Content-Type: application/json")
            override fun findAccounts(@Param("page") page: Int, @Param("pageSize") pageSize: Int): Page<Account>

            @RequestLine("GET $urlPrefix/{id}")
            @Headers("Content-Type: application/json")
            override fun getAccountById(@Param("id") id: UUID): Account

            @RequestLine("POST $urlPrefix/")
            @Headers("Content-Type: application/json")
            override fun saveNewAccount(request: AccountRequest): Account

            @RequestLine("PATCH $urlPrefix/{id}")
            @Headers("Content-Type: application/json")
            override fun updateExistingAccount(@Param("id") id: UUID, request: AccountRequest)
        }
    }

    private val log = KotlinLogging.logger {}

    private val feignClient: AccountServiceFeignApi = Feign.builder()
        .client(client)
        .encoder(JacksonEncoder(objectMapper))
        .decoder(JacksonDecoder(objectMapper))
        .retryer(Retryer.NEVER_RETRY)
        .options(Request.Options(connectTimeoutSeconds, TimeUnit.SECONDS, readTimeoutSeconds, TimeUnit.SECONDS, false))
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

    override fun updateExistingAccount(id: UUID, request: AccountRequest) =
        feignClient.updateExistingAccount(id, request)
}
