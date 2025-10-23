package uk.gov.justice.digital.hmpps.externalmovementsapi.audit

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.EntityTrackingRevisionListener
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
import org.hibernate.envers.RevisionType
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.time.LocalDateTime

@Entity
@Table
@RevisionEntity(AuditRevisionEntityListener::class)
@SequenceGenerator(name = "audit_revision_id_seq", sequenceName = "audit_revision_id_seq", allocationSize = 1)
class AuditRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_revision_id_seq")
  @RevisionNumber
  var id: Long? = null

  // must be called timestamp for EnversRevisionRepositoryImpl
  @RevisionTimestamp
  var timestamp: LocalDateTime? = null

  var username: String? = null

  @Enumerated(EnumType.STRING)
  var source: DataSource? = null

  @JdbcTypeCode(SqlTypes.ARRAY)
  var affectedEntities: MutableSet<String> = sortedSetOf(String.CASE_INSENSITIVE_ORDER)
}

class AuditRevisionEntityListener : EntityTrackingRevisionListener {
  override fun newRevision(revision: Any?) {
    (revision as AuditRevision).apply {
      val context = ExternalMovementContext.get()
      timestamp = context.requestAt
      username = context.username
      source = context.source
    }
  }

  override fun entityChanged(
    entityClass: Class<*>,
    entityName: String,
    entityId: Any,
    revisionType: RevisionType,
    revision: Any,
  ) {
    (revision as AuditRevision).apply {
      affectedEntities.add(entityClass.simpleName)
    }
  }
}
