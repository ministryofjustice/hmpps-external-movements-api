package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PersonReference
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerUpdated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerUpdatedInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.PersonSummaryOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.PersonSummaryOperations.Companion.verifyAgainst
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchServer.Companion.prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonUpdatedHandler

class PrisonerUpdatedIntTest(
  @Autowired @Qualifier("personSummaryOperations") private val ps: PersonSummaryOperations,
  @Autowired private val personUpdateHandler: PersonUpdatedHandler,
) : IntegrationTest(),
  PersonSummaryOperations by ps {
  @ParameterizedTest
  @ValueSource(strings = ["PERSONAL_DETAILS", "LOCATION"])
  fun `person detail is updated when event received`(change: String) {
    val prisonCode = prisonCode()
    val person = givenPersonSummary(personSummary())
    val newDetail = prisoner(prisonCode, person.identifier)
    prisonerSearch.getPrisoners(prisonCode, setOf(person.identifier), listOf(newDetail))

    sendDomainEvent(prisonerUpdatedEvent(person.identifier, setOf(change)))

    waitUntil { (ps.findPersonSummary(person.identifier)?.version ?: 0) > 0 }

    val updated = requireNotNull(ps.findPersonSummary(person.identifier))
    updated.verifyAgainst(newDetail)
  }

  private fun prisonerUpdatedEvent(personIdentifier: String, changes: Set<String>): PrisonerUpdated = PrisonerUpdated(
    PrisonerUpdatedInformation(personIdentifier, changes),
    PersonReference.withIdentifier(personIdentifier),
  )

  @Test
  fun `prisoner updated event ignored if change not of interest`() {
    val prisonCode = prisonCode()
    val person = givenPersonSummary(personSummary())
    prisonerSearch.getPrisoners(prisonCode, setOf(person.identifier))

    personUpdateHandler.handle(
      prisonerUpdatedEvent(
        person.identifier,
        setOf(
          "IDENTIFIERS",
          "ALERTS",
          "SENTENCE",
          "RESTRICTED_PATIENT",
          "INCENTIVE_LEVEL",
          "PHYSICAL_DETAILS",
          "CONTACT_DETAILS",
        ),
      ),
    )

    val unchanged = requireNotNull(ps.findPersonSummary(person.identifier))
    assertThat(unchanged.version).isEqualTo(0)
    assertThat(unchanged.identifier).isEqualTo(person.identifier)
    assertThat(unchanged.firstName).isEqualTo(person.firstName)
    assertThat(unchanged.lastName).isEqualTo(person.lastName)
    assertThat(unchanged.dateOfBirth).isEqualTo(person.dateOfBirth)
    assertThat(unchanged.cellLocation).isEqualTo(person.cellLocation)
  }
}
