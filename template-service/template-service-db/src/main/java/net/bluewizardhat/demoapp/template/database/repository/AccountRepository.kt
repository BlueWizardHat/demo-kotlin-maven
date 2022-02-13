package net.bluewizardhat.demoapp.template.database.repository

import net.bluewizardhat.demoapp.template.database.entity.Account
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AccountRepository : PagingAndSortingRepository<Account, Long> {
    fun findByUuid(uuid: UUID): Optional<Account>
}
