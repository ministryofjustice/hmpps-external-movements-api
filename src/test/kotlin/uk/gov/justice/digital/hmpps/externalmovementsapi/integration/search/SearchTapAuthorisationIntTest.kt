package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

class SearchTapAuthorisationIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(SEARCH_TAP_AUTH_URL)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    searchTapAuthorisations(
      prisonCode(),
      LocalDate.now(),
      LocalDate.now(),
      role = "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `can find authorisations by prison code and date`() {
    val prisonCode = prisonCode()
    val alternativePrisonCode = prisonCode()
    val fromDate = LocalDate.now().plusDays(1)
    val toDate = LocalDate.now().plusDays(3)
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(alternativePrisonCode, fromDate = fromDate, toDate = toDate),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, fromDate = fromDate.minusDays(1), toDate = toDate),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, fromDate = fromDate, toDate = toDate.plusDays(1)),
    )

    val auths = listOf(
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(prisonCode, fromDate = fromDate, toDate = toDate.minusDays(1)),
      ),
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(prisonCode, fromDate = fromDate.plusDays(1), toDate = toDate),
      ),
    )
    prisonerSearch.getPrisoners(prisonCode, auths.map { it.personIdentifier }.toSet())

    val res = searchTapAuthorisations(prisonCode, fromDate, toDate).successResponse<TapAuthorisationSearchResponse>()

    assertThat(res.content.size).isEqualTo(2)
    assertThat(res.metadata.totalElements).isEqualTo(2)
  }

  @Test
  fun `can filter authorisations by status`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDate.now().plusDays(1)
    val toDate = LocalDate.now().plusDays(3)
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.APPROVED,
        fromDate = fromDate,
        toDate = toDate.minusDays(1),
      ),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.APPROVED,
        fromDate = fromDate.plusDays(1),
        toDate = toDate,
      ),
    )

    val auths = listOf(
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          status = TapAuthorisationStatus.Code.PENDING,
          fromDate = fromDate,
          toDate = toDate.minusDays(1),
        ),
      ),
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          status = TapAuthorisationStatus.Code.PENDING,
          fromDate = fromDate.plusDays(1),
          toDate = toDate,
        ),
      ),
    )
    prisonerSearch.getPrisoners(prisonCode, auths.map { it.personIdentifier }.toSet())

    val res = searchTapAuthorisations(prisonCode, fromDate, toDate, TapAuthorisationStatus.Code.PENDING)
      .successResponse<TapAuthorisationSearchResponse>()

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
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.PENDING,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    prisonerSearch.getPrisoners(prisonCode, setOf(toFind.personIdentifier))

    val res = searchTapAuthorisations(prisonCode, fromDate, toDate, personIdentifier = toFind.personIdentifier)
      .successResponse<TapAuthorisationSearchResponse>()

    assertThat(res.content.size).isEqualTo(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
  }

  private fun searchTapAuthorisations(
    prisonCode: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    status: TapAuthorisationStatus.Code? = null,
    personIdentifier: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_TAP_AUTH_URL)
      uri.queryParam("prisonCode", prisonCode)
      uri.queryParam("fromDate", ISO_DATE.format(fromDate))
      uri.queryParam("toDate", ISO_DATE.format(toDate))
      status?.let { uri.queryParam("status", it.name) }
      personIdentifier?.let { uri.queryParam("query", it) }
      uri.build()
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_TAP_AUTH_URL = "/search/temporary-absence-authorisations"
  }
}
