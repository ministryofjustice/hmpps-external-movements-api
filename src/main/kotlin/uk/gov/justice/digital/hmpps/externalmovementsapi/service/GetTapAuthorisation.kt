package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
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
      .map(TemporaryAbsenceOccurrence::asOccurrence)
    return authorisation.with(person, occurrences)
  }
}

private fun TemporaryAbsenceAuthorisation.with(person: Person, occurrences: List<TapOccurrence>) = TapAuthorisation(
  id = id,
  person = person,
  status = status.asCodedDescription(),
  absenceType = absenceType?.asCodedDescription(),
  absenceSubType = absenceSubType?.asCodedDescription(),
  absenceReason = absenceReason?.asCodedDescription(),
  repeat = repeat,
  fromDate = fromDate,
  toDate = toDate,
  occurrences = occurrences,
  submitted = AtAndBy(submittedAt, submittedBy),
  approved = approvedAt?.let { AtAndBy(it, checkNotNull(approvedBy)) },
)

private fun TemporaryAbsenceOccurrence.asOccurrence() = TapOccurrence(
  id = id,
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = Location(type = locationType.asCodedDescription()),
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
)
