package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.UserDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
class GetTapAuthorisation(
  private val prisonerSearch: PrisonerSearchClient,
  private val manageUsers: ManageUsersClient,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun byId(id: UUID): TapAuthorisation {
    val authorisation = authorisationRepository.getAuthorisation(id)
    val person = prisonerSearch.getPrisoner(authorisation.personIdentifier)?.asPerson()
      ?: Person.unknown(authorisation.personIdentifier)
    val occurrences = occurrenceRepository.findByAuthorisationId(authorisation.id)
    val users = manageUsers.getUsersDetails(setOfNotNull(authorisation.submittedBy, authorisation.approvedBy))
      .associateBy { it.username }
    return authorisation.with(
      person,
      occurrences.map { o -> o.asOccurrence() },
    ) { requireNotNull(users[it]) }
  }
}

private fun TemporaryAbsenceAuthorisation.with(
  person: Person,
  occurrences: List<TapAuthorisation.Occurrence>,
  user: (String) -> UserDetails,
) = TapAuthorisation(
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
  submitted = AtAndBy(submittedAt, submittedBy, user(submittedBy).name),
  approved = approvedBy?.let { AtAndBy(checkNotNull(approvedAt), it, user(it).name) },
  schedule = schedule,
  notes = notes,
)

private fun TemporaryAbsenceOccurrence.asOccurrence() = TapAuthorisation.Occurrence(
  id = id,
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  notes = notes,
  isCancelled = cancelledAt != null,
)
