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
  @Column(name = "created_at")
  val createdAt: LocalDateTime,
  @Column(name = "created_by")
  val createdBy: String,
  @Column(name = "updated_at")
  val updatedAt: LocalDateTime?,
  @Column(name = "updated_by")
  val updatedBy: String?,
) : Persistable<UUID> {
  override fun getId() = uuid
  override fun isNew() = true
}

interface MigrationSystemAuditRepository : CrudRepository<MigrationSystemAudit, UUID>
