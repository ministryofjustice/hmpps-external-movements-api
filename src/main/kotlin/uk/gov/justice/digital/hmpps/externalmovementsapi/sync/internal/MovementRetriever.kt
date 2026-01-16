package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.AuditedTapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.AuditedTapMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.getMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndByWithPrison
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapMovement
import java.util.UUID

@Transactional(readOnly = true)
@Service
class MovementRetriever(
  private val auditedMovements: AuditedTapMovementRepository,
) {
  fun getById(id: UUID): TapMovement = auditedMovements.getMovement(id).forSync()
}

private fun AuditedTapMovement.forSync() = TapMovement(
  id = id,
  occurrenceId = occurrence?.id,
  personIdentifier = personIdentifier,
  occurredAt = occurredAt,
  direction = direction,
  absenceReasonCode = absenceReason.code,
  location = location,
  accompaniedByCode = accompaniedBy.code,
  accompaniedByComments = accompaniedByComments,
  comments = comments,
  created = AtAndByWithPrison(createdAt, createdBy, recordedByPrisonCode),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)
