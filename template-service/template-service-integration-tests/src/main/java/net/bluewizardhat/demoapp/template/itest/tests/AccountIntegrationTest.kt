package net.bluewizardhat.demoapp.template.itest.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import feign.okhttp.OkHttpClient
import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountOperations
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.api.Page
import net.bluewizardhat.demoapp.template.client.AccountFeignClient
import net.bluewizardhat.demoapp.template.itest.IntegrationTestsProperties
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountIntegrationTest {
    private val accountClient: AccountOperations by lazy { createAccountClient() }

    @Test
    fun getAccountById() {
        // Setup
        val expectedUuid = UUID.fromString("2b441c6f-e9d4-4252-ab66-f87998080d51")

        // Execute
        val actual = accountClient.getAccountById(expectedUuid)

        // Verify
        assertEquals(expectedUuid, actual.id)
        assertEquals("Test Account", actual.name)
    }

    @Test
    fun createAndRetrieve() {
        val request = AccountRequest(name = "New Account - ${UUID.randomUUID()}")

        val savedAccount = accountClient.saveNewAccount(request)
        val retrievedAccount = accountClient.getAccountById(savedAccount.id)

        assertEquals(savedAccount.id, retrievedAccount.id)
        assertEquals(savedAccount.name, retrievedAccount.name)

        val page: Page<Account> = accountClient.findAccounts(0, 100)
        assertTrue(page.content.contains(retrievedAccount))
    }

    private fun createAccountClient(): AccountOperations {
        val client = OkHttpClient()
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(ParameterNamesModule())
            .registerModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISO timestamps please

        return AccountFeignClient(IntegrationTestsProperties.endpoint, objectMapper, client)
    }
}
