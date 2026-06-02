package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.AuditedTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.AuditedTapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.AuditedTapOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.read.TapOccurrence
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
  absenceTypeCode = absenceType?.code,
  absenceSubTypeCode = absenceSubType?.code ?: if (absenceType?.code == AbsenceType.Code.SECURITY_ESCORT.value) AbsenceSubType.Code.SECURITY_ESCORT.value else null,
  absenceReasonCode = absenceReason.code,
  accompaniedByCode = accompaniedBy.code,
  transportCode = transport.code,
  contactInformation = contactInformation,
  comments = comments,
  created = AtAndBy(createdAt, createdBy),
  updated = updatedAt?.let { AtAndBy(it, updatedBy!!) },
)

private fun AuditedTapAuthorisation.forOccurrence() = TapOccurrence.Authorisation(id = id, personIdentifier = personIdentifier, prisonCode = prisonCode)
