package uk.gov.justice.digital.hmpps.externalmovementsapi.audit

import org.hibernate.envers.RevisionType

class AuditedEntity<T>(
  val type: RevisionType,
  val revision: AuditRevision,
  val state: T,
)
