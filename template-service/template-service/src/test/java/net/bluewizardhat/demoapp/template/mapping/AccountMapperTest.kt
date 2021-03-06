package net.bluewizardhat.demoapp.template.mapping

import net.bluewizardhat.demoapp.template.api.Account
import net.bluewizardhat.demoapp.template.api.AccountRequest
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toApi
import net.bluewizardhat.demoapp.template.mapping.AccountMapper.toEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import net.bluewizardhat.demoapp.template.database.entity.Account as AccountEntity

internal class AccountMapperTest {
    @Test
    internal fun testNewAccountRequestToEntity() {
        // Setup
        val request = AccountRequest("AccountName")
        val expectedResult = AccountEntity(
            name = request.name ?: ""
        )

        // Execute
        val actualResult = request.toEntity()

        // Verify
        assertEquals(expectedResult.name, actualResult.name)
        assertNotNull(actualResult.created)
        assertNotNull(actualResult.updated)
    }

    @Test
    internal fun testEntityToApi() {
        // Setup
        val name = "AccountName"
        val id = UUID.randomUUID()
        val created = OffsetDateTime.now()
        val updated = created.plusHours(3)
        val entity = AccountEntity(
            name = name,
            id = id,
            created = created,
            updated = updated
        )
        val expectedResult = Account(
            name = name,
            id = id,
            created = created,
            updated = updated
        )

        // Execute
        val actualResult = entity.toApi()

        // Verify
        assertEquals(expectedResult, actualResult)
    }
}
