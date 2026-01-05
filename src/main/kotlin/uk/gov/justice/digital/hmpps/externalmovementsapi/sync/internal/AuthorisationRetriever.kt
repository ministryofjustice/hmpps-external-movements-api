package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.AuditedTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.AuditedTapAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.AuditedTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.AuditedTapOccurrenceRepository
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
    return authorisation.with(occurrences.map { o -> o.asOccurrence(authorisation) })
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
  accompaniedByCode = accompaniedBy.code,
  repeat = repeat,
  start = start,
  end = end,
  comments = comments,
  occurrences = occurrences,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)

private fun AuditedTapOccurrence.asOccurrence(authorisation: AuditedTapAuthorisation) = TapAuthorisation.Occurrence(
  id = id,
  statusCode = status.code,
  start = start,
  end = end,
  location = location,
  absenceTypeCode = authorisation.absenceType?.code,
  absenceSubTypeCode = authorisation.absenceSubType?.code,
  absenceReasonCode = requireNotNull(authorisation.absenceReason).code,
  accompaniedByCode = accompaniedBy.code,
  transportCode = transport.code,
  comments = comments,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)
