package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.AuditedTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.AuditedTapAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.AuditedTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.AuditedTapOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapAuthorisation
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AuthorisationRetriever(
  private val authorisationRepository: AuditedTapAuthorisationRepository,
  private val occurrenceRepository: AuditedTapOccurrenceRepository,
) {
  fun getById(id: UUID): TapAuthorisation {
    val authorisation = authorisationRepository.getAuthorisation(id)
    val occurrences = occurrenceRepository.findByAuthorisationId(authorisation.id)
    return authorisation.with(occurrences.map { o -> o.asOccurrence() })
  }
}

private fun AuditedTapAuthorisation.with(
  occurrences: List<TapAuthorisation.Occurrence>,
) = TapAuthorisation(
  id = id,
  personIdentifier = personIdentifier,
  prisonCode = prisonCode,
  statusCode = status.code,
  absenceTypeCode = absenceType?.code,
  absenceSubTypeCode = absenceSubType?.code,
  absenceReasonCode = requireNotNull(absenceReason).code,
  repeat = repeat,
  fromDate = fromDate,
  toDate = toDate,
  notes = notes,
  occurrences = occurrences,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)

private fun AuditedTapOccurrence.asOccurrence() = TapAuthorisation.Occurrence(
  id = id,
  statusCode = status.code,
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = location,
  accompaniedByCode = accompaniedBy.code,
  transportCode = transport.code,
  notes = notes,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)
