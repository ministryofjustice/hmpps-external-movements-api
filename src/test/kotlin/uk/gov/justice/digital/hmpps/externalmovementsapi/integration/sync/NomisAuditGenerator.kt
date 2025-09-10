package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest.Companion.DEFAULT_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.NomisAudit
import java.time.LocalDateTime

object NomisAuditGenerator {
  fun generate(
    createdAt: LocalDateTime = LocalDateTime.now().minusHours(1),
    createdBy: String = DEFAULT_USERNAME,
    modifiedBy: String? = SYSTEM_USERNAME,
    modifiedAt: LocalDateTime? = LocalDateTime.now(),
    auditTimestamp: LocalDateTime? = modifiedAt,
    auditUserId: String? = modifiedBy,
  ): NomisAudit = NomisAudit(createdAt, createdBy, modifiedBy, modifiedAt, auditTimestamp, auditUserId)
}
