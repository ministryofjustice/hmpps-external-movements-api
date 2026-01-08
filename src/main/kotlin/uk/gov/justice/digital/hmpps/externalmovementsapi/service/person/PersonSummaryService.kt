package uk.gov.justice.digital.hmpps.externalmovementsapi.service.person

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient

@Service
@Transactional
class PersonSummaryService(
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  fun findPersonSummary(personIdentifier: String): PersonSummary? = personSummaryRepository.findByIdOrNull(personIdentifier)

  fun getWithSave(personIdentifier: String): PersonSummary = findPersonSummary(personIdentifier)
    ?: (prisonerSearch.getPrisoner(personIdentifier)?.let { save(it) })
    ?: throw NotFoundException("Prisoner not found")

  fun updateExistingDetails(prisonNumber: String) {
    personSummaryRepository.findByIdOrNull(prisonNumber)?.also {
      val prisoner = requireNotNull(prisonerSearch.getPrisoner(prisonNumber)) { "Prisoner number invalid" }
      it.update(
        prisoner.firstName,
        prisoner.lastName,
        prisoner.dateOfBirth,
        prisoner.cellLocation,
      )
    }
  }

  fun save(person: Prisoner): PersonSummary = personSummaryRepository.findByIdOrNull(person.prisonerNumber)
    ?.update(person.firstName, person.lastName, person.dateOfBirth, person.cellLocation)
    ?: personSummaryRepository.save(person.summary())

  private fun Prisoner.summary() = PersonSummary(firstName, lastName, dateOfBirth, cellLocation, prisonerNumber)
}
