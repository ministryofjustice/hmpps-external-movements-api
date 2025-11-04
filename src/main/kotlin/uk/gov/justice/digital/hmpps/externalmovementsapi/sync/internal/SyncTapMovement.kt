package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovement
import java.util.UUID

@Transactional
@Service
class SyncTapMovement(
  private val referenceDataRepository: ReferenceDataRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(personIdentifier: String, request: TapMovement): SyncResponse {
    val occurrence = request.occurrenceId?.let { occurrenceRepository.getOccurrence(it) }?.also {
      require(personIdentifier == it.personIdentifier) { "Person identifier does not match occurrence" }
    }
    val rdProvider = referenceDataRepository.rdProvider(request)
    val movement =
      (request.id?.let { movementRepository.findByIdOrNull(it) } ?: movementRepository.findByLegacyId(request.legacyId))
        ?.update(personIdentifier, occurrence, request, rdProvider)
        ?: movementRepository.save(request.asEntity(personIdentifier, occurrence, rdProvider))
    return SyncResponse(movement.id)
  }

  fun deleteById(id: UUID) {
    movementRepository.findByIdOrNull(id)?.also(movementRepository::delete)
  }

  private fun TapMovement.asEntity(
    personIdentifier: String,
    occurrence: TemporaryAbsenceOccurrence?,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = TemporaryAbsenceMovement(
    personIdentifier = personIdentifier,
    occurrence = occurrence,
    occurredAt = occurredAt,
    direction = valueOf(direction.name),
    absenceReason = rdProvider(ReferenceDataDomain.Code.ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
    accompaniedByNotes = accompaniedByNotes,
    notes = notes,
    recordedAt = recorded.at,
    recordedBy = recorded.by,
    recordedByPrisonCode = recorded.prisonCode,
    location = location,
    legacyId = legacyId,
    id = id ?: newUuid(),
  )
}
