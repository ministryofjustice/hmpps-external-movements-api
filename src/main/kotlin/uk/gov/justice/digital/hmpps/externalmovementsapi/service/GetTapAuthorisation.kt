package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
class GetTapAuthorisation(
  private val prisonerSearch: PrisonerSearchClient,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun byId(id: UUID): TapAuthorisation {
    val authorisation = authorisationRepository.getAuthorisation(id)
    val person = prisonerSearch.getPrisoner(authorisation.personIdentifier)?.asPerson()
      ?: Person.unknown(authorisation.personIdentifier)
    val occurrences = occurrenceRepository.findByAuthorisationId(authorisation.id)
    return authorisation.with(person, occurrences.map { o -> o.asOccurrence() })
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
  repeat = repeat,
  fromDate = fromDate,
  toDate = toDate,
  occurrences = occurrences,
  schedule = schedule,
  notes = notes,
)

private fun TemporaryAbsenceOccurrence.asOccurrence() = TapAuthorisation.Occurrence(
  id = id,
  status = status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  notes = notes,
)
