package net.bluewizardhat.demoapp.template.database.repository

import net.bluewizardhat.demoapp.template.database.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<Account, UUID>
