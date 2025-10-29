package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.findRdWithPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapApplicationRequest

@Transactional
@Service
class SyncTapApplication(
  private val referenceDataRepository: ReferenceDataRepository,
  private val tapAuthRepository: TemporaryAbsenceAuthorisationRepository,
) {
  fun sync(personIdentifier: String, request: TapApplicationRequest): SyncResponse {
    ExternalMovementContext.get().copy(source = DataSource.NOMIS).set()
    val rdPaths = referenceDataRepository.findRdWithPaths(request)
    val application = (
      request.id?.let { tapAuthRepository.findByIdOrNull(it) }
        ?: tapAuthRepository.findByLegacyId(request.movementApplicationId)
      )
      ?.update(personIdentifier, request, rdPaths)
      ?: tapAuthRepository.save(request.asEntity(personIdentifier, rdPaths))
    return SyncResponse(application.id)
  }

  private fun TapApplicationRequest.asEntity(
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
      prisonCode = requireNotNull(prisonId),
      absenceType = temporaryAbsenceType?.let { rdPaths.getReferenceData(ABSENCE_TYPE, it) as AbsenceType },
      absenceSubType = temporaryAbsenceSubType?.let {
        rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, eventSubType) as AbsenceReason,
      repeat = isRepeating(),
      status = rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, tapAuthStatusCode.name) as TapAuthorisationStatus,
      notes = comment,
      fromDate = fromDate,
      toDate = toDate,
      applicationDate = applicationDate,
      submittedAt = audit.createDatetime,
      submittedBy = audit.createUsername,
      approvedAt = approvedAt,
      approvedBy = approvedBy,
      schedule = null,
      reasonPath = reasonPath,
      legacyId = movementApplicationId,
    )
  }
}
