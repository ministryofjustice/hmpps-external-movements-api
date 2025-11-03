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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.ScheduledTemporaryAbsenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.SyncResponse
import java.util.UUID

@Transactional
@Service
class SyncScheduledTemporaryAbsence(
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun sync(parentId: UUID, request: ScheduledTemporaryAbsenceRequest): SyncResponse {
    ExternalMovementContext.get().copy(source = DataSource.NOMIS).set()
    val authorisation = authorisationRepository.getAuthorisation(parentId)
    val rdProvider = referenceDataRepository.rdProvider(request)
    val occurrence =
      (
        request.id?.let { occurrenceRepository.findByIdOrNull(it) }
          ?: occurrenceRepository.findByLegacyId(request.eventId)
        )
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
    contactInformation = contactPersonName,
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, escortOrDefault()) as AccompaniedBy,
    transport = rdProvider(ReferenceDataDomain.Code.TRANSPORT, transportTypeOrDefault()) as Transport,
    location = location.asLocation(),
    notes = comment,
    addedAt = audit.createDatetime,
    addedBy = audit.createUsername,
    cancelledAt = cancelledAt,
    cancelledBy = cancelledBy,
    legacyId = eventId,
    scheduleReference = null,
    id = id ?: newUuid(),
  )
}
