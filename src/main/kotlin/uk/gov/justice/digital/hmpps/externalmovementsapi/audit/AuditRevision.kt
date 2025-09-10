package uk.gov.justice.digital.hmpps.externalmovementsapi.audit

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.envers.RevisionEntity
import org.hibernate.envers.RevisionListener
import org.hibernate.envers.RevisionNumber
import org.hibernate.envers.RevisionTimestamp
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
  var caseloadId: String? = null

  @Enumerated(EnumType.STRING)
  var source: DataSource? = null
}

class AuditRevisionEntityListener : RevisionListener {
  override fun newRevision(revision: Any?) {
    (revision as AuditRevision).apply {
      val context = ExternalMovementContext.get()
      timestamp = context.requestAt
      username = context.username
      caseloadId = context.activeCaseloadId
      source = context.source
    }
  }
}
