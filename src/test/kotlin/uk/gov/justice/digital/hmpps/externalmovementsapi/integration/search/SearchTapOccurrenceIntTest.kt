package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.now

class SearchTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

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

    val res = searchTapOccurrences(prisonCode, query = toFind.person.identifier)
      .successResponse<TapOccurrenceSearchResponse>()

    assertThat(res.content.size).isEqualTo(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
  }

  @Test
  fun `can find by person name`() {
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

    toFind.person.nameFormats().forEach {
      val res = searchTapOccurrences(prisonCode, query = it)
        .successResponse<TapOccurrenceSearchResponse>()

      assertThat(res.content.size).isEqualTo(1)
      assertThat(res.metadata.totalElements).isEqualTo(1)
    }
  }

  @Test
  fun `can filter occurrences by status`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDateTime.now().plusDays(1)
    val toDate = LocalDateTime.now().plusDays(2)
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ1 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1, releaseAt = fromDate, returnBy = toDate))
    assertThat(occ1.status.code).isEqualTo(TapOccurrenceStatus.Code.SCHEDULED.name)

    val auth2 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.PENDING,
      ),
    )
    val occ2 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2, releaseAt = fromDate, returnBy = toDate))
    assertThat(occ2.status.code).isEqualTo(TapOccurrenceStatus.Code.PENDING.name)

    val auth3 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ3 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth3,
        releaseAt = LocalDateTime.now().minusDays(3),
        returnBy = LocalDateTime.now().minusDays(1),
      ),
    )
    assertThat(occ3.status.code).isEqualTo(TapOccurrenceStatus.Code.EXPIRED.name)

    val auth4 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.DENIED,
      ),
    )
    val occ4 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth4, releaseAt = fromDate, returnBy = toDate))
    assertThat(occ4.status.code).isEqualTo(TapOccurrenceStatus.Code.DENIED.name)

    val res = searchTapOccurrences(
      prisonCode,
      statuses = listOf(TapOccurrenceStatus.Code.SCHEDULED, TapOccurrenceStatus.Code.EXPIRED),
    ).successResponse<TapOccurrenceSearchResponse>()

    assertThat(res.content.size).isEqualTo(2)
    assertThat(res.metadata.totalElements).isEqualTo(2)
    assertThat(res.content.map { it.status.code }).containsExactlyInAnyOrder(
      TapOccurrenceStatus.Code.SCHEDULED.name,
      TapOccurrenceStatus.Code.EXPIRED.name,
    )

    val single = searchTapOccurrences(
      prisonCode,
      statuses = listOf(TapOccurrenceStatus.Code.DENIED),
    ).successResponse<TapOccurrenceSearchResponse>()

    assertThat(single.content.size).isEqualTo(1)
    assertThat(single.metadata.totalElements).isEqualTo(1)
    assertThat(single.content.map { it.status.code }).containsExactlyInAnyOrder(TapOccurrenceStatus.Code.DENIED.name)
  }

  @Test
  fun `can sort occurrences by status`() {
    val prisonCode = prisonCode()
    val fromDate = LocalDateTime.now().plusDays(1)
    val toDate = LocalDateTime.now().plusDays(2)
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ1 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1, releaseAt = fromDate, returnBy = toDate))
    assertThat(occ1.status.code).isEqualTo(TapOccurrenceStatus.Code.SCHEDULED.name)

    val auth2 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.PENDING,
      ),
    )
    val occ2 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2, releaseAt = fromDate, returnBy = toDate))
    assertThat(occ2.status.code).isEqualTo(TapOccurrenceStatus.Code.PENDING.name)

    val auth3 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ3 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth3,
        releaseAt = LocalDateTime.now().minusDays(3),
        returnBy = LocalDateTime.now().minusDays(1),
      ),
    )
    assertThat(occ3.status.code).isEqualTo(TapOccurrenceStatus.Code.EXPIRED.name)

    val auth4 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = TapAuthorisationStatus.Code.DENIED,
      ),
    )
    val occ4 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth4, releaseAt = fromDate, returnBy = toDate))
    assertThat(occ4.status.code).isEqualTo(TapOccurrenceStatus.Code.DENIED.name)

    val auth5 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ5 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth5,
        releaseAt = LocalDateTime.now().minusDays(3),
        returnBy = LocalDateTime.now().minusDays(1),
        movements = listOf(
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.OUT,
            auth5.person.identifier,
            occurredAt = LocalDateTime.now().minusDays(3),
            recordedByPrison = prisonCode,
          ),
        ),
      ),
    )
    assertThat(occ5.status.code).isEqualTo(TapOccurrenceStatus.Code.OVERDUE.name)

    val auth6 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ6 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth6,
        releaseAt = LocalDateTime.now().minusHours(1),
        returnBy = LocalDateTime.now().plusHours(3),
        movements = listOf(
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.OUT,
            auth6.person.identifier,
            occurredAt = LocalDateTime.now().minusHours(1),
            recordedByPrison = prisonCode,
          ),
        ),
      ),
    )
    assertThat(occ6.status.code).isEqualTo(TapOccurrenceStatus.Code.IN_PROGRESS.name)

    val res1 = searchTapOccurrences(prisonCode, sort = "status,asc").successResponse<TapOccurrenceSearchResponse>()
    assertThat(res1.content.size).isEqualTo(6)
    assertThat(res1.metadata.totalElements).isEqualTo(6)
    assertThat(res1.content.map { it.status.code }).containsExactly(
      TapOccurrenceStatus.Code.OVERDUE.name,
      TapOccurrenceStatus.Code.IN_PROGRESS.name,
      TapOccurrenceStatus.Code.SCHEDULED.name,
      TapOccurrenceStatus.Code.PENDING.name,
      TapOccurrenceStatus.Code.EXPIRED.name,
      TapOccurrenceStatus.Code.DENIED.name,
    )

    val res2 = searchTapOccurrences(prisonCode, sort = "status,desc").successResponse<TapOccurrenceSearchResponse>()
    assertThat(res2.content.size).isEqualTo(6)
    assertThat(res2.metadata.totalElements).isEqualTo(6)
    assertThat(res2.content.map { it.status.code }).containsExactly(
      TapOccurrenceStatus.Code.DENIED.name,
      TapOccurrenceStatus.Code.EXPIRED.name,
      TapOccurrenceStatus.Code.PENDING.name,
      TapOccurrenceStatus.Code.SCHEDULED.name,
      TapOccurrenceStatus.Code.IN_PROGRESS.name,
      TapOccurrenceStatus.Code.OVERDUE.name,
    )
  }

  private fun searchTapOccurrences(
    prisonCode: String,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
    query: String? = null,
    statuses: List<TapOccurrenceStatus.Code>? = null,
    sort: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_TAP_OCCURRENCES_URL)
      uri.queryParam("prisonCode", prisonCode)
      fromDate?.also { uri.queryParam("fromDate", it) }
      toDate?.also { uri.queryParam("toDate", it) }
      statuses?.also { uri.queryParam("status", *it.toTypedArray()) }
      sort?.also { uri.queryParam("sort", it) }
      query?.let { uri.queryParam("query", it) }
      uri.build()
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_TAP_OCCURRENCES_URL = "/search/temporary-absence-occurrences"
  }
}
