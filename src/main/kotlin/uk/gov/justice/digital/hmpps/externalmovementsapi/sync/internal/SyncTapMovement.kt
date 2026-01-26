package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementDirection
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementOccurredAt
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovement
import java.util.UUID
import kotlin.reflect.KClass

@Transactional
@Service
class SyncTapMovement(
  private val personSummaryService: PersonSummaryService,
  private val referenceDataRepository: ReferenceDataRepository,
  private val occurrenceStatusRepository: OccurrenceStatusRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(personIdentifier: String, request: TapMovement): SyncResponse {
    val occurrence = request.occurrenceId?.let { occurrenceRepository.getOccurrence(it) }?.also {
      require(personIdentifier == it.person.identifier) { "Person identifier does not match occurrence" }
    }
    val rdProvider = referenceDataRepository.rdProvider()
    val person = occurrence?.person ?: let { personSummaryService.getWithSave(personIdentifier) }
    val movement =
      (request.id?.let { movementRepository.findByIdOrNull(it) } ?: movementRepository.findByLegacyId(request.legacyId))
        ?.also {
          request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
        }
        ?.update(occurrence, request, rdProvider)
        ?: let {
          ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
          val movement = request.asEntity(person, occurrence, rdProvider)
          occurrence?.addMovement(movement) {
            rdProvider(OccurrenceStatus::class, it) as OccurrenceStatus
          }
          movementRepository.save(movement)
        }
    return SyncResponse(movement.id)
  }

  fun deleteById(id: UUID) {
    movementRepository.findByIdOrNull(id)?.also {
      it.occurrence?.removeMovement(it) { statusCode -> occurrenceStatusRepository.getByCode(statusCode) }
      movementRepository.delete(it)
    }
  }

  private fun TapMovement.asEntity(
    person: PersonSummary,
    occurrence: TemporaryAbsenceOccurrence?,
    rdProvider: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = TemporaryAbsenceMovement(
    person = person,
    occurrence = occurrence?.calculateStatus { rdProvider(OccurrenceStatus::class, it) as OccurrenceStatus },
    occurredAt = occurredAt,
    direction = valueOf(direction.name),
    prisonCode = requireNotNull(prisonCode ?: created.prisonCode),
    absenceReason = rdProvider(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
    accompaniedBy = rdProvider(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
    accompaniedByComments = accompaniedByComments,
    comments = comments,
    location = location,
    legacyId = legacyId,
    id = id ?: newUuid(),
  )

  private fun TemporaryAbsenceMovement.update(
    occurrence: TemporaryAbsenceOccurrence?,
    request: TapMovement,
    rdProvider: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) = apply {
    check(occurrence?.id == this.occurrence?.id) {
      "Attempt to move movement to another occurrence"
    }
    applyDirection(ChangeMovementDirection(request.direction))
    applyOccurredAt(ChangeMovementOccurredAt(request.occurredAt))
    applyLocation(ChangeMovementLocation(request.location))
    request.comments?.also { applyComments(ChangeMovementComments(it)) }
    applyAccompaniedBy(
      ChangeMovementAccompaniment(
        request.accompaniedByCode,
        request.accompaniedByComments,
      ),
      rdProvider,
    )
    applyReason(ChangeMovementReason(request.absenceReasonCode), rdProvider)
  }
}
