package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.forAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.startAfter
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.startBefore
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.time.LocalDate
import java.util.UUID

@Service
class GetTapAuthorisation(
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun byId(id: UUID, start: LocalDate?, end: LocalDate?): TapAuthorisation {
    val authorisation = authorisationRepository.getAuthorisation(id)
    val totalOccurrences = occurrenceRepository.count(forAuthorisation(authorisation.id))
    val occurrences: List<TemporaryAbsenceOccurrence> = occurrenceRepository.findAll(
      listOfNotNull(
        forAuthorisation(authorisation.id),
        start?.let { startAfter(it.atStartOfDay()) },
        end?.let { startBefore(it.plusDays(1).atStartOfDay()) },
      ).reduce(Specification<TemporaryAbsenceOccurrence>::and),
    )
    return authorisation.with(authorisation.person.asPerson(), totalOccurrences, occurrences.map { o -> o.asOccurrence() })
  }
}

private fun TemporaryAbsenceAuthorisation.with(
  person: Person,
  totalOccurrences: Long,
  occurrences: List<TapAuthorisation.Occurrence>,
) = TapAuthorisation(
  id = id,
  person = person,
  status = status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  repeat = repeat,
  start = start,
  end = end,
  totalOccurrenceCount = totalOccurrences,
  occurrences = occurrences,
  locations = locations,
  schedule = schedule,
  comments = comments,
)

private fun TemporaryAbsenceOccurrence.asOccurrence() = TapAuthorisation.Occurrence(
  id = id,
  status = status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  start = start,
  end = end,
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  comments = comments,
)
