package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.repository.CrudRepository
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "migration_system_audit")
class MigrationSystemAudit(
  @Id
  @Column("id")
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val createdBy: String,
  var updatedAt: LocalDateTime?,
  var updatedBy: String?,
) : Persistable<UUID> {
  override fun getId() = uuid

  @Transient
  var new: Boolean = true
  override fun isNew() = new
}

interface MigrationSystemAuditRepository : CrudRepository<MigrationSystemAudit, UUID>
