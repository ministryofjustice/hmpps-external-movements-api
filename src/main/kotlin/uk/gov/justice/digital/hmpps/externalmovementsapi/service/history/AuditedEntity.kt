package uk.gov.justice.digital.hmpps.externalmovementsapi.service.history

import org.hibernate.envers.RevisionType
import uk.gov.justice.digital.hmpps.externalmovementsapi.audit.AuditRevision

class AuditedEntity<T>(
  val type: RevisionType,
  val revision: AuditRevision,
  val state: T,
)
