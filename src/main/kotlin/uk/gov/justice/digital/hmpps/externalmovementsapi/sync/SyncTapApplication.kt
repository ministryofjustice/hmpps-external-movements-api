package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceSeries
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceSeriesRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import java.time.LocalDateTime
import java.time.LocalTime

@Transactional
@Service
class SyncTapApplication(
  private val referenceDataRepository: ReferenceDataRepository,
  private val seriesRepository: TemporaryAbsenceSeriesRepository,
) {
  fun sync(personIdentifier: String, request: TapApplicationRequest): SyncResponse {
    ExternalMovementContext.get().copy(source = DataSource.NOMIS).set()
    val rdMap =
      referenceDataRepository.findByKeyIn(request.requiredReferenceData().map { it.first of it.second }.toSet())
        .associateBy { it.key }
    val rdProvider = { dc: ReferenceDataDomain.Code, c: String -> requireNotNull(rdMap[dc of c]) }
    val application =
      seriesRepository.findByLegacyId(request.movementApplicationId)?.update(personIdentifier, request, rdProvider)
        ?: seriesRepository.save(request.asEntity(personIdentifier, rdProvider))
    return SyncResponse(application.id)
  }

  fun TapApplicationRequest.asEntity(
    personIdentifier: String,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = TemporaryAbsenceSeries(
    personIdentifier = personIdentifier,
    prisonCode = requireNotNull(prisonId),
    absenceType = temporaryAbsenceType?.let { rdProvider(ABSENCE_TYPE, it) as AbsenceType },
    absenceSubType = temporaryAbsenceSubType?.let { rdProvider(ABSENCE_SUB_TYPE, it) as AbsenceSubType },
    absenceReason = rdProvider(ABSENCE_REASON, eventSubType) as AbsenceReason,
    repeat = isRepeating(),
    releaseAt = releaseTime,
    returnBy = returnTime,
    locationType = toAddressOwnerClass?.let { rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, it) as? LocationType }
      ?: rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, "OTHER") as LocationType,
    locationId = toAddressId?.toString(),
    accompanied = isAccompanied(),
    accompaniedBy = escortCode?.let { rdProvider(ACCOMPANIED_BY, it) as AccompaniedBy },
    transport = transportType?.let { rdProvider(TRANSPORT, it) as Transport },
    status = rdProvider(TAP_STATUS, applicationStatus) as TapStatus,
    notes = comment,
    submittedAt = LocalDateTime.of(applicationDate, LocalTime.MIN),
    legacyId = movementApplicationId,
    toAgencyCode = toAgencyId,
  )
}
