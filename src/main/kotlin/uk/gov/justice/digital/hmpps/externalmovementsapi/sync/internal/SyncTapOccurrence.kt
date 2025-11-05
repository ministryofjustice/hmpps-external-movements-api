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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
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
    val rdProvider = referenceDataRepository.rdProvider(request)
    val occurrence =
      (
        request.id?.let { occurrenceRepository.findByIdOrNull(it) }
          ?: occurrenceRepository.findByLegacyId(request.legacyId)
        )
        ?.also {
          request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
        }
        ?.update(request, rdProvider)
        ?: let {
          ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
          occurrenceRepository.save(request.asEntity(authorisation, rdProvider))
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
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ) = TemporaryAbsenceOccurrence(
    authorisation = authorisation,
    releaseAt = releaseAt,
    returnBy = returnBy,
    contactInformation = null,
    accompaniedBy = rdProvider(ReferenceDataDomain.Code.ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
    transport = rdProvider(ReferenceDataDomain.Code.TRANSPORT, transportCode) as Transport,
    location = location,
    notes = notes,
    addedAt = created.at,
    addedBy = created.by,
    cancelledAt = updated?.at,
    cancelledBy = updated?.by,
    legacyId = legacyId,
    scheduleReference = null,
    id = id ?: newUuid(),
  )
}
