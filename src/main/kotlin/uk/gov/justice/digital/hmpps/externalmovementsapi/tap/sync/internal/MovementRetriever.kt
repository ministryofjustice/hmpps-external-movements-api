package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.AuditedTapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.AuditedTapMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.getMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.read.TapMovement
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
  prisonCode = prisonCode,
  absenceReasonCode = absenceReason.code,
  location = location,
  accompaniedByCode = accompaniedBy.code,
  accompaniedByComments = accompaniedByComments,
  comments = comments,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)
