package net.bluewizardhat.demoapp.template.database.repository

import net.bluewizardhat.demoapp.template.database.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<Account, UUID> {
    @Modifying
    @Query("update Account a set a.name = :name, updated = now() where a.id = :id")
    fun updateAccount(id: UUID, name: String): Int
}
