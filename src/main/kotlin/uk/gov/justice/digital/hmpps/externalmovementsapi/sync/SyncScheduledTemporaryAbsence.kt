package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import java.util.UUID

@Transactional
@Service
class SyncScheduledTemporaryAbsence(
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun sync(parentId: UUID, request: ScheduledTemporaryAbsenceRequest): SyncResponse {
    val authorisation = authorisationRepository.getAuthorisation(parentId)
    val rdMap =
      referenceDataRepository.findByKeyIn(request.requiredReferenceData().map { it.first of it.second }.toSet())
        .associateBy { it.key }
    val rdProvider = { dc: ReferenceDataDomain.Code, c: String -> requireNotNull(rdMap[dc of c]) }
    val occurrence =
      (request.id?.let { occurrenceRepository.findByIdOrNull(it) } ?: occurrenceRepository.findByLegacyId(request.eventId))
        ?.update(request, rdProvider)
        ?: occurrenceRepository.save(request.asEntity(authorisation, rdProvider))
    return SyncResponse(occurrence.id)
  }

  private fun ScheduledTemporaryAbsenceRequest.asEntity(
    authorisation: TemporaryAbsenceAuthorisation,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = TemporaryAbsenceOccurrence(
    authorisation = authorisation,
    releaseAt = startTime,
    returnBy = returnTime,
    status = rdProvider(
      ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS,
      occurrenceStatusCode.name,
    ) as TapOccurrenceStatus,
    locationType = toAddressOwnerClass?.let { rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, it) as? LocationType }
      ?: rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, "OTHER") as LocationType,
    locationId = toAddressId?.toString(),
    contact = contactPersonName,
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, escortOrDefault()) as AccompaniedBy,
    transport = rdProvider(ReferenceDataDomain.Code.TRANSPORT, transportTypeOrDefault()) as Transport,
    notes = comment,
    addedAt = audit.createDatetime,
    addedBy = audit.createUsername,
    cancelledAt = cancelledAt,
    cancelledBy = cancelledBy,
    legacyId = eventId,
    id = id ?: newUuid(),
  )
}
