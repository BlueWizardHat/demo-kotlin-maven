package net.bluewizardhat.demoapp.template.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Sort
import java.time.OffsetDateTime
import java.util.UUID

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
