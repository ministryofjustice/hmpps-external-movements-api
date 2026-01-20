package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.AuditedTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.AuditedTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.AuditedTapOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapOccurrence
import java.util.UUID

@Transactional(readOnly = true)
@Service
class OccurrenceRetriever(
  private val occurrenceRepository: AuditedTapOccurrenceRepository,
) {
  fun getById(id: UUID): TapOccurrence = occurrenceRepository.getOccurrence(id).forSync()
}

private fun AuditedTapOccurrence.forSync() = TapOccurrence(
  id = id,
  authorisation = authorisation.forOccurrence(),
  statusCode = status.code,
  start = start,
  end = end,
  location = location,
  absenceTypeCode = authorisation.absenceType?.code,
  absenceSubTypeCode = authorisation.absenceSubType?.code ?: if (absenceType?.code == "SE") "SE" else null,
  absenceReasonCode = requireNotNull(authorisation.absenceReason).code,
  accompaniedByCode = accompaniedBy.code,
  transportCode = transport.code,
  contactInformation = contactInformation,
  comments = comments,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)

private fun AuditedTapAuthorisation.forOccurrence() = TapOccurrence.Authorisation(id = id, personIdentifier = personIdentifier, prisonCode = prisonCode)
