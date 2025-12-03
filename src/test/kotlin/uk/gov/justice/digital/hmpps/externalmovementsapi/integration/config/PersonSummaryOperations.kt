package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.assertj.core.api.Assertions.assertThat
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.cellLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.dob
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import java.time.LocalDate

interface PersonSummaryOperations {
  fun givenPersonSummary(personSummary: PersonSummary): PersonSummary
  fun findPersonSummary(personIdentifier: String): PersonSummary?

  fun personSummary(
    personIdentifier: String = personIdentifier(),
    firstName: String = word(8),
    lastName: String = word(8),
    dateOfBirth: LocalDate = dob(),
    cellLocation: String? = cellLocation(),
  ): PersonSummary = PersonSummary(firstName, lastName, dateOfBirth, cellLocation, personIdentifier)

  companion object {
    fun PersonSummary.verifyAgainst(prisoner: Prisoner) {
      assertThat(identifier).isEqualTo(prisoner.prisonerNumber)
      assertThat(firstName).isEqualTo(prisoner.firstName)
      assertThat(lastName).isEqualTo(prisoner.lastName)
      assertThat(dateOfBirth).isEqualTo(prisoner.dateOfBirth)
      assertThat(cellLocation).isEqualTo(prisoner.cellLocation)
    }
  }
}

class PersonSummaryOperationsImpl(
  private val personSummaryRepository: PersonSummaryRepository,
) : PersonSummaryOperations {
  override fun givenPersonSummary(personSummary: PersonSummary): PersonSummary = personSummaryRepository.save(personSummary)

  override fun findPersonSummary(personIdentifier: String): PersonSummary? = personSummaryRepository.findByIdOrNull(personIdentifier)
}
