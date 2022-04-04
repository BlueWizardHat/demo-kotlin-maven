package net.bluewizardhat.demoapp.template.itest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import feign.okhttp.OkHttpClient
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.client.AccountFeignClient
import net.bluewizardhat.demoapp.template.itest.Main.Companion.baseUrl
import java.util.UUID
import kotlin.test.Test

class AccountIntegrationTest {
    private val accountClient: AccountOperations = createAccountClient()

    @Test
    fun getAccountById() {
        val account = accountClient.getAccountById(UUID.fromString("2b441c6f-e9d4-4252-ab66-f87998080d51"))
        println("$account")
    }

    private fun createAccountClient(): AccountOperations {
        val client = OkHttpClient()
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(ParameterNamesModule())
            .registerModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISO timestamps please

        return AccountFeignClient(baseUrl, objectMapper, client)
    }
}
