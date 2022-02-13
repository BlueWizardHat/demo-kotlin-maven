package net.bluewizardhat.demoapp.template.database.entity

import org.hibernate.annotations.Type
import org.springframework.data.domain.Sort
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.Size

@Entity
@Table(name = "account")
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Type(type = "pg-uuid")
    @Size(min = 36, max = 36)
    @Column(name = "uuid", nullable = false, length = 36)
    val uuid: UUID = UUID.randomUUID(),

    @Size(max = 64)
    @Column(name = "name", nullable = false, length = 64)
    val name: String,

    @Column(name = "created", nullable = false)
    val created: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated", nullable = false)
    val updated: OffsetDateTime = OffsetDateTime.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long? = null
) {
    companion object {
        val defaultSort: Sort = Sort.by(Sort.Order.desc("created"))
    }
}
