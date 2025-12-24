package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
class GetTapOccurrence(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun byId(id: UUID): TapOccurrence {
    val occurrence = occurrenceRepository.getOccurrence(id)
    val pac = occurrenceRepository.getPosition(occurrence.authorisation.id, occurrence.id)
    return occurrence.toModel(pac.position, pac.total)
  }
}

private fun TemporaryAbsenceAuthorisation.forOccurrence() = TapOccurrence.Authorisation(
  id = id,
  person = person.asPerson(),
  status = status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  accompaniedBy = accompaniedBy.asCodedDescription(),
  repeat = repeat,
  comments = comments,
)

private fun TemporaryAbsenceOccurrence.toModel(position: Int, total: Int) = TapOccurrence(
  id = id,
  status = status.asCodedDescription(),
  authorisation = authorisation.forOccurrence(),
  absenceType = absenceType
    ?.takeIf { reasonPath.has(ABSENCE_TYPE) }
    ?.asCodedDescription(),
  absenceSubType = absenceSubType
    ?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }
    ?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory
    ?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason
    ?.takeIf { reasonPath.has(ABSENCE_REASON) }
    ?.asCodedDescription(),
  start = start,
  end = end,
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  contactInformation = contactInformation,
  scheduleReference = scheduleReference,
  comments = comments,
  occurrencePosition = position,
  totalOccurrences = total,
)
