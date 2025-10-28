package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
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
    val rdProvider = referenceDataRepository.rdProvider(request)
    val application = (request.id?.let { tapAuthRepository.findByIdOrNull(it) } ?: tapAuthRepository.findByLegacyId(request.movementApplicationId))
      ?.update(personIdentifier, request, rdProvider)
      ?: tapAuthRepository.save(request.asEntity(personIdentifier, rdProvider))
    return SyncResponse(application.id)
  }

  private fun TapApplicationRequest.asEntity(
    personIdentifier: String,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = TemporaryAbsenceAuthorisation(
    id = id ?: newUuid(),
    personIdentifier = personIdentifier,
    prisonCode = requireNotNull(prisonId),
    absenceType = temporaryAbsenceType?.let { rdProvider(ABSENCE_TYPE, it) as AbsenceType },
    absenceSubType = temporaryAbsenceSubType?.let { rdProvider(ABSENCE_SUB_TYPE, it) as AbsenceSubType },
    absenceReasonCategory = null,
    absenceReason = rdProvider(ABSENCE_REASON, eventSubType) as AbsenceReason,
    repeat = isRepeating(),
    status = rdProvider(TAP_AUTHORISATION_STATUS, tapAuthStatusCode.name) as TapAuthorisationStatus,
    notes = comment,
    fromDate = fromDate,
    toDate = toDate,
    applicationDate = applicationDate,
    submittedAt = audit.createDatetime,
    submittedBy = audit.createUsername,
    approvedAt = approvedAt,
    approvedBy = approvedBy,
    schedule = null,
    reasonPath = ReasonPath(reasonPath()),
    legacyId = movementApplicationId,
  )
}
