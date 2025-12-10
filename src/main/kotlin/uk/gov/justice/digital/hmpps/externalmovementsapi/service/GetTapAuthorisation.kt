package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.forAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.startAfter
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.startBefore
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
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
    val occurrences = occurrenceRepository.findAll(
      listOfNotNull(
        forAuthorisation(authorisation.id),
        start?.let { startAfter(it) },
        end?.let { startBefore(it.plusDays(1)) },
      ).reduce { spec, current -> spec.and(current) },
    )
    return authorisation.with(authorisation.person.asPerson(), occurrences.map { o -> o.asOccurrence() })
  }
}

private fun TemporaryAbsenceAuthorisation.with(
  person: Person,
  occurrences: List<TapAuthorisation.Occurrence>,
) = TapAuthorisation(
  id = id,
  person = person,
  status = status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  repeat = repeat,
  start = start,
  end = end,
  occurrences = occurrences,
  locations = occurrences.map { it.location }.distinct(),
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
  absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  start = start,
  end = end,
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  comments = comments,
)
