package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.migration

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
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
  val updatedAt: LocalDateTime?,
  val updatedBy: String?,
) : Persistable<UUID> {
  override fun getId() = uuid
  override fun isNew() = true
}

interface MigrationSystemAuditRepository : CrudRepository<MigrationSystemAudit, UUID>
