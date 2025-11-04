package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapAuthorisation
import java.util.UUID

@Transactional(readOnly = true)
@Service
class AuthorisationRetriever(
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun getById(id: UUID): TapAuthorisation {
    val authorisation = authorisationRepository.getAuthorisation(id)
    val occurrences = occurrenceRepository.findByAuthorisationId(authorisation.id)
    return authorisation.with(occurrences.map { o -> o.asOccurrence() })
  }
}

private fun TemporaryAbsenceAuthorisation.with(
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
  submitted = AtAndBy(submittedAt, submittedBy),
  notes = notes,
  occurrences = occurrences,
)

private fun TemporaryAbsenceOccurrence.asOccurrence() = TapAuthorisation.Occurrence(
  id = id,
  statusCode = requireNotNull(status).code,
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = location,
  accompaniedByCode = accompaniedBy.code,
  transportCode = transport.code,
  notes = notes,
)
