package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.now
import java.time.format.DateTimeFormatter.ISO_DATE

class SearchTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(SEARCH_TAP_OCCURRENCES_URL)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    searchTapOccurrences(
      prisonCode(),
      LocalDate.now(),
      LocalDate.now(),
      role = "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `can find occurrences by prison code and date`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDate.now().plusDays(1)
    val toDate = LocalDate.now().plusDays(10)

    val authorisations = (1..5).map {
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode = if (it in listOf(2, 3)) prisonCode else prisonCode(),
          fromDate = fromDate.minusDays(1),
          toDate = toDate.plusDays(1),
        ),
      )
    }
    authorisations.mapIndexed { idx, auth ->
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          releaseAt = LocalDateTime.of(fromDate, now()).plusDays(idx.toLong()),
          returnBy = LocalDateTime.of(toDate, now()).minusDays(idx.toLong()),
        ),
      )
    }
    prisonerSearch.getPrisoners(prisonCode, listOf(authorisations[1], authorisations[2]).map { it.person.identifier }.toSet())

    val res = searchTapOccurrences(prisonCode, fromDate, toDate).successResponse<TapOccurrenceSearchResponse>()

    assertThat(res.content.size).isEqualTo(2)
    assertThat(res.metadata.totalElements).isEqualTo(2)
  }

  @Test
  fun `can find by prison number`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDate.now().plusDays(1)
    val toDate = LocalDate.now().plusDays(3)

    val toFind = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.PENDING,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        toFind,
        releaseAt = LocalDateTime.of(fromDate, now()),
        returnBy = LocalDateTime.of(toDate, now()),
      ),
    )
    val doNotFind = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.PENDING,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        doNotFind,
        releaseAt = LocalDateTime.of(fromDate, now()),
        returnBy = LocalDateTime.of(toDate, now()),
      ),
    )
    prisonerSearch.getPrisoners(prisonCode, setOf(toFind.person.identifier))

    val res = searchTapOccurrences(prisonCode, fromDate, toDate, personIdentifier = toFind.person.identifier)
      .successResponse<TapOccurrenceSearchResponse>()

    assertThat(res.content.size).isEqualTo(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
  }

  private fun searchTapOccurrences(
    prisonCode: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    personIdentifier: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_TAP_OCCURRENCES_URL)
      uri.queryParam("prisonCode", prisonCode)
      uri.queryParam("fromDate", ISO_DATE.format(fromDate))
      uri.queryParam("toDate", ISO_DATE.format(toDate))
      personIdentifier?.let { uri.queryParam("query", it) }
      uri.build()
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_TAP_OCCURRENCES_URL = "/search/temporary-absence-occurrences"
  }
}
