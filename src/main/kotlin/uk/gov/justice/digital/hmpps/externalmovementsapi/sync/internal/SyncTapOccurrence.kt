package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceContactInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapOccurrence
import java.util.UUID

@Transactional
@Service
class SyncTapOccurrence(
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(authorisationId: UUID, request: TapOccurrence): SyncResponse {
    val authorisation = authorisationRepository.getAuthorisation(authorisationId)
    val rdPaths = referenceDataRepository.referenceDataFor(request)
    val occurrence =
      (
        request.id?.let { occurrenceRepository.findByIdOrNull(it) }
          ?: occurrenceRepository.findByLegacyId(request.legacyId)
        )
        ?.also {
          request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
        }
        ?.update(request, rdPaths, authorisation)
        ?: let {
          if (authorisation.status.code != AuthorisationStatus.Code.APPROVED.name) {
            throw ConflictException("Attempt to add occurrence to a non-approved authorisation")
          }
          ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
          if (!authorisation.repeat) {
            occurrenceRepository.findByAuthorisationId(authorisation.id).singleOrNull()?.also {
              occurrenceRepository.delete(it)
            }
          }
          occurrenceRepository.save(
            request.asEntity(authorisation, rdPaths).calculateStatus {
              rdPaths.getReferenceData(OccurrenceStatus::class, it) as OccurrenceStatus
            },
          )
        }
    occurrenceRepository.flush()
    val locations = occurrenceRepository.findByAuthorisationId(authorisation.id).mapTo(linkedSetOf()) { it.location }
    authorisation.applyLocations(ChangeAuthorisationLocations(locations))
    return SyncResponse(occurrence.id)
  }

  fun deleteById(id: UUID) {
    ExternalMovementContext.get().copy(username = SYSTEM_USERNAME).set()
    occurrenceRepository.findByIdOrNull(id)?.also { occurrence ->
      val movementCount = movementRepository.countByOccurrenceId(occurrence.id)
      if (movementCount > 0) {
        throw ConflictException("Cannot delete an occurrence with a movement")
      } else {
        occurrenceRepository.delete(occurrence)
      }
    }
  }

  private fun TapOccurrence.asEntity(
    authorisation: TemporaryAbsenceAuthorisation,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceOccurrence {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain.clazz, it.code)
    }
    return TemporaryAbsenceOccurrence(
      authorisation = authorisation,
      absenceType = absenceTypeCode?.let {
        rdPaths.getReferenceData(
          AbsenceType::class,
          it,
        ) as AbsenceType
      },
      absenceSubType = absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value }?.let {
        rdPaths.getReferenceData(AbsenceSubType::class, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
      start = start,
      end = end,
      contactInformation = contactInformation,
      accompaniedBy = rdPaths.getReferenceData(
        AccompaniedBy::class,
        accompaniedByCode,
      ) as AccompaniedBy,
      transport = rdPaths.getReferenceData(Transport::class, transportCode) as Transport,
      location = location,
      comments = comments,
      legacyId = legacyId,
      reasonPath = reasonPath,
      scheduleReference = null,
      id = id ?: newUuid(),
    ).apply {
      if (isCancelled) {
        cancel(CancelOccurrence(), rdPaths::getReferenceData)
      }
    }
  }

  private fun TemporaryAbsenceOccurrence.update(
    request: TapOccurrence,
    rdPaths: ReferenceDataPaths,
    authorisation: TemporaryAbsenceAuthorisation,
  ) = apply {
    authorisationPersonAndPrison(authorisation)
    applyAbsenceCategorisation(request, rdPaths)
    applySchedule(request)
    applyLogistics(request, rdPaths)
    checkCancellation(request, rdPaths)
    applyComments(ChangeOccurrenceComments(request.comments))
    if (request.isCancelled && movements().isEmpty()) {
      cancel(CancelOccurrence(), rdPaths::getReferenceData)
    } else {
      calculateStatus {
        rdPaths.getReferenceData(OccurrenceStatus::class, it) as OccurrenceStatus
      }
    }
  }

  private fun TemporaryAbsenceOccurrence.applyAbsenceCategorisation(
    request: TapOccurrence,
    rdPaths: ReferenceDataPaths,
  ) {
    val categoryCode = rdPaths.reasonPath().path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.code
    applyAbsenceCategorisation(
      RecategoriseOccurrence(
        request.absenceTypeCode,
        request.absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value },
        categoryCode,
        request.absenceReasonCode,
        rdPaths.reasonPath(),
      ),
      rdPaths::getReferenceData,
    )
  }

  private fun TemporaryAbsenceOccurrence.applySchedule(request: TapOccurrence) {
    reschedule(RescheduleOccurrence(request.start, request.end))
  }

  private fun TemporaryAbsenceOccurrence.applyLogistics(request: TapOccurrence, rdPaths: ReferenceDataPaths) {
    applyLocation(ChangeOccurrenceLocation(request.location))
    applyAccompaniment(ChangeOccurrenceAccompaniment(request.accompaniedByCode), rdPaths::getReferenceData)
    applyTransport(ChangeOccurrenceTransport(request.transportCode), rdPaths::getReferenceData)
    request.contactInformation?.also { applyContactInformation(ChangeOccurrenceContactInformation(it)) }
  }

  private fun TemporaryAbsenceOccurrence.checkCancellation(request: TapOccurrence, rdPaths: ReferenceDataPaths) {
    if (request.isCancelled) {
      cancel(CancelOccurrence(), rdPaths::getReferenceData)
    }
  }
}
