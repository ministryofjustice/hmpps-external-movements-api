package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.getMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapMovement
import java.util.UUID

@Service
class MovementRetriever(private val movementRepository: TemporaryAbsenceMovementRepository) {
  fun getById(id: UUID): TapMovement = movementRepository.getMovement(id).forSync()
}

private fun TemporaryAbsenceMovement.forSync() = TapMovement(
  id = id,
  occurrenceId = occurrence?.id,
  occurredAt = occurredAt,
  direction = direction,
  absenceReasonCode = absenceReason.code,
  location = location,
  accompaniedByCode = accompaniedBy.code,
  accompaniedByNotes = accompaniedByNotes,
  notes = notes,
  recordedByPrisonCode = recordedByPrisonCode,
)
