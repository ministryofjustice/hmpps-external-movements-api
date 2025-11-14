package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
class GetTapOccurrence(
  private val prisonerSearch: PrisonerSearchClient,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun byId(id: UUID): TapOccurrence {
    val occurrence = occurrenceRepository.getOccurrence(id)
    val person = prisonerSearch.getPrisoner(occurrence.authorisation.personIdentifier)?.asPerson()
      ?: Person.unknown(occurrence.authorisation.personIdentifier)
    return occurrence.with(person)
  }
}

private fun TemporaryAbsenceAuthorisation.with(person: Person) = TapOccurrence.Authorisation(
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
  notes = notes,
)

private fun TemporaryAbsenceOccurrence.with(person: Person) = TapOccurrence(
  id = id,
  status = status.asCodedDescription(),
  authorisation = authorisation.with(person),
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
  contactInformation = contactInformation,
  scheduleReference = scheduleReference,
  notes = notes,
)
