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
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

class SearchTapAuthorisationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
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
    prisonerSearch.getPrisoners(prisonCode, auths.map { it.person.identifier }.toSet())

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
    prisonerSearch.getPrisoners(prisonCode, auths.map { it.person.identifier }.toSet())

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
    prisonerSearch.getPrisoners(prisonCode, setOf(toFind.person.identifier))

    val res = searchTapAuthorisations(prisonCode, fromDate, toDate, personIdentifier = toFind.person.identifier)
      .successResponse<TapAuthorisationSearchResponse>()

    assertThat(res.content.size).isEqualTo(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
  }

  @Test
  fun `can sort by name`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDate.now().plusDays(1)
    val toDate = LocalDate.now().plusDays(2)

    val p1 = givenPersonSummary(personSummary(lastName = "Smith", firstName = "Jane"))
    val p2 = givenPersonSummary(personSummary(lastName = "Doe", firstName = "John"))
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        p1.identifier,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        p2.identifier,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )

    val res1 = searchTapAuthorisations(prisonCode, fromDate, toDate, sort = "firstName,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)

    assertThat(res1.content.map { it.person.personIdentifier }).containsExactly(p1.identifier, p2.identifier)

    val res2 = searchTapAuthorisations(prisonCode, fromDate, toDate, sort = "lastName,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)

    assertThat(res2.content.map { it.person.personIdentifier }).containsExactly(p2.identifier, p1.identifier)

    val res3 = searchTapAuthorisations(prisonCode, fromDate, toDate, sort = "firstName,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res3.content.size).isEqualTo(2)
    assertThat(res3.metadata.totalElements).isEqualTo(2)

    assertThat(res3.content.map { it.person.personIdentifier }).containsExactly(p2.identifier, p1.identifier)

    val res4 = searchTapAuthorisations(prisonCode, fromDate, toDate, sort = "lastName,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res4.content.size).isEqualTo(2)
    assertThat(res4.metadata.totalElements).isEqualTo(2)

    assertThat(res4.content.map { it.person.personIdentifier }).containsExactly(p1.identifier, p2.identifier)
  }

  @Test
  fun `can sort by status`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDate.now().plusDays(1)
    val toDate = LocalDate.now().plusDays(2)

    val pending = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.PENDING,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    val cancelled = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.CANCELLED,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    val denied = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.DENIED,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )
    val approved = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.APPROVED,
        fromDate = fromDate,
        toDate = toDate,
      ),
    )

    val res1 = searchTapAuthorisations(prisonCode, fromDate, toDate, sort = "status,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res1.content.size).isEqualTo(4)
    assertThat(res1.metadata.totalElements).isEqualTo(4)
    println(res1.content)

    assertThat(res1.content.map { it.person.personIdentifier }).containsExactly(
      pending.person.identifier,
      approved.person.identifier,
      denied.person.identifier,
      cancelled.person.identifier,
    )

    val res2 = searchTapAuthorisations(prisonCode, fromDate, toDate, sort = "status,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res2.content.size).isEqualTo(4)
    assertThat(res2.metadata.totalElements).isEqualTo(4)

    assertThat(res2.content.map { it.person.personIdentifier }).containsExactly(
      cancelled.person.identifier,
      denied.person.identifier,
      approved.person.identifier,
      pending.person.identifier,
    )
  }

  private fun searchTapAuthorisations(
    prisonCode: String,
    fromDate: LocalDate,
    toDate: LocalDate,
    status: TapAuthorisationStatus.Code? = null,
    personIdentifier: String? = null,
    sort: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_TAP_AUTH_URL)
      uri.queryParam("prisonCode", prisonCode)
      uri.queryParam("fromDate", ISO_DATE.format(fromDate))
      uri.queryParam("toDate", ISO_DATE.format(toDate))
      status?.also { uri.queryParam("status", it.name) }
      personIdentifier?.also { uri.queryParam("query", it) }
      sort?.also { uri.queryParam("sort", it) }
      uri.build()
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_TAP_AUTH_URL = "/search/temporary-absence-authorisations"
  }
}
