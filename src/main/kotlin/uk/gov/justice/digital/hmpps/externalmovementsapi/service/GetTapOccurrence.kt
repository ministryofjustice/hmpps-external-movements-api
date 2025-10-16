package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers.UserDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
class GetTapOccurrence(
  private val prisonerSearch: PrisonerSearchClient,
  private val manageUsers: ManageUsersClient,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun byId(id: UUID): TapOccurrence {
    val occurrence = occurrenceRepository.getOccurrence(id)
    val person = prisonerSearch.getPrisoner(occurrence.authorisation.personIdentifier)?.asPerson()
      ?: Person.unknown(occurrence.authorisation.personIdentifier)
    val users = manageUsers.getUsersDetails(setOfNotNull(occurrence.addedBy, occurrence.cancelledBy))
      .associateBy { it.username }
    return occurrence.with(person) { requireNotNull(users[it]) }
  }
}

private fun TemporaryAbsenceAuthorisation.with(person: Person) = TapOccurrence.Authorisation(
  id = id,
  person = person,
  status = status.asCodedDescription(),
  absenceType = absenceType?.asCodedDescription(),
  absenceSubType = absenceSubType?.asCodedDescription(),
  absenceReason = absenceReason?.asCodedDescription(),
)

private fun TemporaryAbsenceOccurrence.with(person: Person, user: (String) -> UserDetails) = TapOccurrence(
  id = id,
  authorisation = authorisation.with(person),
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  added = AtAndBy(addedAt, addedBy, user(addedBy).name),
  cancelled = cancelledBy?.let { AtAndBy(checkNotNull(cancelledAt), it, user(it).name) },
  contactInformation = contactInformation,
  scheduleReference = scheduleReference,
)
