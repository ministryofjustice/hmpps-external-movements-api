package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal.EntityType.ABSENCE
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapOccurrence
import java.util.UUID

@Transactional
@Service
class SyncTapOccurrence(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(request: TapOccurrence): Pair<SyncResponse, SyncRequestStatus> {
    val occurrence =
      (
        request.id?.let { occurrenceRepository.findByIdOrNull(it) }
          ?: occurrenceRepository.findByLegacyId(request.legacyId)
        )

    val status = occurrence?.let {
      SyncRequestStatus.Actionable(ActionType.UPDATE, ABSENCE)
    } ?: let { SyncRequestStatus.Actionable(ActionType.CREATE, ABSENCE) }

    return occurrence?.id?.let {
      SyncResponse(it) to status
    } ?: throw ConflictException("Create occurrence request not expected")
  }

  fun deleteById(id: UUID): SyncRequestStatus {
    ExternalMovementContext.get().copy(username = SYSTEM_USERNAME).set()
    return occurrenceRepository.findByIdOrNull(id)?.takeUnless { it.dpsOnly }?.let { occurrence ->
      val movementCount = movementRepository.countByOccurrenceId(occurrence.id)
      if (movementCount > 0) {
        SyncRequestStatus.NotActionable(ActionType.DELETE, ABSENCE, "Occurrence has $movementCount movements")
      } else {
        SyncRequestStatus.Actionable(ActionType.DELETE, ABSENCE)
      }
    } ?: SyncRequestStatus.NotFound(ActionType.DELETE, ABSENCE)
  }
}
