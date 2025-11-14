package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.findRdWithPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapAuthorisation
import java.util.UUID

@Transactional
@Service
class SyncTapAuthorisation(
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(personIdentifier: String, request: TapAuthorisation): SyncResponse {
    val rdPaths = referenceDataRepository.findRdWithPaths(request)
    val application = (
      request.id?.let { authorisationRepository.findByIdOrNull(it) }
        ?: authorisationRepository.findByLegacyId(request.legacyId)
      )
      ?.also {
        request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
      }
      ?.update(personIdentifier, request, rdPaths)
      ?: let {
        ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
        authorisationRepository.save(request.asEntity(personIdentifier, rdPaths))
      }
    return SyncResponse(application.id)
  }

  fun deleteById(id: UUID) {
    authorisationRepository.findByIdOrNull(id)?.also { authorisation ->
      val occurrences = occurrenceRepository.findByAuthorisationId(authorisation.id)
      val occurrenceIds = occurrences.map { it.id }.toSet()
      movementRepository.findByOccurrenceIdIn(occurrenceIds).also { movementRepository.deleteAll(it) }
      occurrenceRepository.deleteAll(occurrences)
      authorisationRepository.delete(authorisation)
    }
  }

  private fun TapAuthorisation.asEntity(
    personIdentifier: String,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceAuthorisation {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain, it.code)
    }
    return TemporaryAbsenceAuthorisation(
      id = id ?: newUuid(),
      personIdentifier = personIdentifier,
      prisonCode = prisonCode,
      absenceType = absenceTypeCode?.let { rdPaths.getReferenceData(ABSENCE_TYPE, it) as AbsenceType },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
      accompaniedBy = rdPaths.getReferenceData(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
      repeat = repeat,
      status = rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, statusCode) as TapAuthorisationStatus,
      notes = notes,
      fromDate = fromDate,
      toDate = toDate,
      schedule = null,
      reasonPath = reasonPath,
      legacyId = legacyId,
    )
  }
}
