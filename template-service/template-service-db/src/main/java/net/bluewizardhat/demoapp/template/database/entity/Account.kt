package net.bluewizardhat.demoapp.template.database.entity

import org.springframework.data.domain.Sort
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.Size

@Entity
@Table(name = "account")
data class Account(
    @field:Id
    @field:GeneratedValue
    @field:Column(name = "id", nullable = false)
    val id: UUID? = null,

    @field:Size(max = 64)
    @field:Column(name = "name", nullable = false, length = 64)
    val name: String,

    @field:Column(name = "created", nullable = false)
    val created: OffsetDateTime = OffsetDateTime.now(),

    @field:Column(name = "updated", nullable = false)
    val updated: OffsetDateTime = OffsetDateTime.now(),

    @field:Version
    @field:Column(name = "version", nullable = false)
    var version: Long? = null
) {
    companion object {
        val defaultSort: Sort = Sort.by(Sort.Order.asc("created"))
    }
}
