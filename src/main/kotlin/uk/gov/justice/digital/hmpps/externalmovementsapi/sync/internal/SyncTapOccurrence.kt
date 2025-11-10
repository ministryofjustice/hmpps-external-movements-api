package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.findRdWithPaths
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
    val rdPaths = referenceDataRepository.findRdWithPaths(request)
    val occurrence =
      (
        request.id?.let { occurrenceRepository.findByIdOrNull(it) }
          ?: occurrenceRepository.findByLegacyId(request.legacyId)
        )
        ?.also {
          request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
        }
        ?.update(request, rdPaths)
        ?: let {
          ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
          occurrenceRepository.save(
            request.asEntity(authorisation, rdPaths).calculateStatus {
              rdPaths.getReferenceData(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus
            },
          )
        }
    return SyncResponse(occurrence.id)
  }

  fun deleteById(id: UUID) {
    occurrenceRepository.findByIdOrNull(id)?.also { occurrence ->
      movementRepository.findByOccurrenceId(occurrence.id).also { movementRepository.deleteAll(it) }
      occurrenceRepository.delete(occurrence)
    }
  }

  private fun TapOccurrence.asEntity(
    authorisation: TemporaryAbsenceAuthorisation,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceOccurrence {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain, it.code)
    }
    return TemporaryAbsenceOccurrence(
      authorisation = authorisation,
      absenceType = absenceTypeCode?.let {
        rdPaths.getReferenceData(
          ReferenceDataDomain.Code.ABSENCE_TYPE,
          it,
        ) as AbsenceType
      },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
      releaseAt = releaseAt,
      returnBy = returnBy,
      contactInformation = null,
      accompaniedBy = rdPaths.getReferenceData(
        ReferenceDataDomain.Code.ACCOMPANIED_BY,
        accompaniedByCode,
      ) as AccompaniedBy,
      transport = rdPaths.getReferenceData(ReferenceDataDomain.Code.TRANSPORT, transportCode) as Transport,
      location = location,
      notes = notes,
      addedAt = created.at,
      addedBy = created.by,
      cancelledAt = updated?.at,
      cancelledBy = updated?.by,
      legacyId = legacyId,
      reasonPath = reasonPath,
      scheduleReference = null,
      id = id ?: newUuid(),
    )
  }
}
