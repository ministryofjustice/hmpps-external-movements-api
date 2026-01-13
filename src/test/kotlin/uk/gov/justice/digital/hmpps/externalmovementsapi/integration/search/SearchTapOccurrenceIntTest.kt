package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.postcode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.location
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

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, EXTERNAL_MOVEMENTS_RO])
  fun `403 forbidden without correct role`(role: String) {
    searchTapOccurrences(
      prisonCode(),
      LocalDate.now(),
      LocalDate.now(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `can find occurrences by date`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now().minusDays(2)
    val end = LocalDate.now().plusDays(2)

    val authorisations = (1..5).map {
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode = prisonCode,
          start = start.minusDays(1),
          end = end.plusDays(1),
        ),
      )
    }
    authorisations.mapIndexed { idx, auth ->
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          start = LocalDateTime.of(start.minusDays(2), now()).plusDays(idx.toLong()),
          end = LocalDateTime.of(start.minusDays(1), now()).plusDays(idx.toLong()),
        ),
      )
    }

    val res = searchTapOccurrences(prisonCode, start, end).successResponse<TapOccurrenceSearchResponse>()

    assertThat(res.content.size).isEqualTo(4)
    assertThat(res.metadata.totalElements).isEqualTo(4)
  }

  @Test
  fun `can find by prison number`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now().plusDays(1)
    val end = LocalDate.now().plusDays(3)

    val toFind = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        toFind,
        start = LocalDateTime.of(start, now()),
        end = LocalDateTime.of(end, now()),
      ),
    )
    val doNotFind = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        doNotFind,
        start = LocalDateTime.of(start, now()),
        end = LocalDateTime.of(end, now()),
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
    val start = LocalDate.now().plusDays(1)
    val end = LocalDate.now().plusDays(3)

    val toFind = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        toFind,
        start = LocalDateTime.of(start, now()),
        end = LocalDateTime.of(end, now()),
      ),
    )
    val doNotFind = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        doNotFind,
        start = LocalDateTime.of(start, now()),
        end = LocalDateTime.of(end, now()),
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
    val start = LocalDateTime.now().minusDays(3)
    val end = LocalDateTime.now().plusDays(1)
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1, start = start, end = end))
    assertThat(occ1.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)

    val auth2 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
      ),
    )
    val occ2 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2, start = start, end = end))
    assertThat(occ2.status.code).isEqualTo(OccurrenceStatus.Code.PENDING.name)

    val auth3 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ3 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth3,
        start = LocalDateTime.now().minusDays(3),
        end = LocalDateTime.now().minusDays(1),
      ),
    )
    assertThat(occ3.status.code).isEqualTo(OccurrenceStatus.Code.EXPIRED.name)

    val auth4 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.DENIED,
      ),
    )
    val occ4 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth4, start = start, end = end))
    assertThat(occ4.status.code).isEqualTo(OccurrenceStatus.Code.DENIED.name)

    val res = searchTapOccurrences(
      prisonCode,
      start = start.toLocalDate(),
      end = end.toLocalDate(),
      statuses = listOf(OccurrenceStatus.Code.SCHEDULED, OccurrenceStatus.Code.EXPIRED),
    ).successResponse<TapOccurrenceSearchResponse>()

    assertThat(res.content.size).isEqualTo(2)
    assertThat(res.metadata.totalElements).isEqualTo(2)
    assertThat(res.content.map { it.status.code }).containsExactlyInAnyOrder(
      OccurrenceStatus.Code.SCHEDULED.name,
      OccurrenceStatus.Code.EXPIRED.name,
    )

    val single = searchTapOccurrences(
      prisonCode,
      statuses = listOf(OccurrenceStatus.Code.DENIED),
    ).successResponse<TapOccurrenceSearchResponse>()

    assertThat(single.content.size).isEqualTo(1)
    assertThat(single.metadata.totalElements).isEqualTo(1)
    assertThat(single.content.map { it.status.code }).containsExactlyInAnyOrder(OccurrenceStatus.Code.DENIED.name)
  }

  @Test
  fun `can sort occurrences by status`() {
    val prisonCode = prisonCode()
    val start = LocalDateTime.now().minusDays(3)
    val end = LocalDateTime.now().plusDays(3)
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ1 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1, start = start, end = end))
    assertThat(occ1.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)

    val auth2 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
      ),
    )
    val occ2 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2, start = start, end = end))
    assertThat(occ2.status.code).isEqualTo(OccurrenceStatus.Code.PENDING.name)

    val auth3 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ3 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth3,
        start = LocalDateTime.now().minusDays(3),
        end = LocalDateTime.now().minusDays(1),
      ),
    )
    assertThat(occ3.status.code).isEqualTo(OccurrenceStatus.Code.EXPIRED.name)

    val auth4 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.DENIED,
      ),
    )
    val occ4 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth4, start = start, end = end))
    assertThat(occ4.status.code).isEqualTo(OccurrenceStatus.Code.DENIED.name)

    val auth5 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ5 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth5,
        start = LocalDateTime.now().minusDays(3),
        end = LocalDateTime.now().minusDays(1),
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
    assertThat(occ5.status.code).isEqualTo(OccurrenceStatus.Code.OVERDUE.name)

    val auth6 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ6 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth6,
        start = LocalDateTime.now().minusHours(1),
        end = LocalDateTime.now().plusHours(3),
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
    assertThat(occ6.status.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)

    val res1 = searchTapOccurrences(
      prisonCode,
      start = start.toLocalDate(),
      end = end.toLocalDate(),
      sort = "status,asc",
    ).successResponse<TapOccurrenceSearchResponse>()

    assertThat(res1.content.size).isEqualTo(6)
    assertThat(res1.metadata.totalElements).isEqualTo(6)
    assertThat(res1.content.map { it.status.code }).containsExactly(
      OccurrenceStatus.Code.OVERDUE.name,
      OccurrenceStatus.Code.IN_PROGRESS.name,
      OccurrenceStatus.Code.SCHEDULED.name,
      OccurrenceStatus.Code.PENDING.name,
      OccurrenceStatus.Code.EXPIRED.name,
      OccurrenceStatus.Code.DENIED.name,
    )

    val res2 = searchTapOccurrences(
      prisonCode,
      start = start.toLocalDate(),
      end = end.toLocalDate(),
      sort = "status,desc",
    ).successResponse<TapOccurrenceSearchResponse>()

    assertThat(res2.content.size).isEqualTo(6)
    assertThat(res2.metadata.totalElements).isEqualTo(6)
    assertThat(res2.content.map { it.status.code }).containsExactly(
      OccurrenceStatus.Code.DENIED.name,
      OccurrenceStatus.Code.EXPIRED.name,
      OccurrenceStatus.Code.PENDING.name,
      OccurrenceStatus.Code.SCHEDULED.name,
      OccurrenceStatus.Code.IN_PROGRESS.name,
      OccurrenceStatus.Code.OVERDUE.name,
    )
  }

  @Test
  fun `can sort by name`() {
    val prisonCode = prisonCode()

    val p1 = givenPersonSummary(personSummary(lastName = "Smith", firstName = "Jane"))
    val p2 = givenPersonSummary(personSummary(lastName = "Doe", firstName = "John"))
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, p1.identifier))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1))
    val auth2 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, p2.identifier))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2))

    val res1 = searchTapOccurrences(prisonCode, sort = "firstName,asc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)

    assertThat(res1.content.map { it.authorisation.person.personIdentifier }).containsExactly(
      p1.identifier,
      p2.identifier,
    )

    val res2 = searchTapOccurrences(prisonCode, sort = "lastName,asc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)

    assertThat(res2.content.map { it.authorisation.person.personIdentifier }).containsExactly(
      p2.identifier,
      p1.identifier,
    )

    val res3 = searchTapOccurrences(prisonCode, sort = "firstName,desc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res3.content.size).isEqualTo(2)
    assertThat(res3.metadata.totalElements).isEqualTo(2)

    assertThat(res3.content.map { it.authorisation.person.personIdentifier }).containsExactly(
      p2.identifier,
      p1.identifier,
    )

    val res4 = searchTapOccurrences(prisonCode, sort = "lastName,desc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res4.content.size).isEqualTo(2)
    assertThat(res4.metadata.totalElements).isEqualTo(2)

    assertThat(res4.content.map { it.authorisation.person.personIdentifier }).containsExactly(
      p1.identifier,
      p2.identifier,
    )
  }

  @Test
  fun `can sort type or reason`() {
    val prisonCode = prisonCode()

    val sr = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(sr))
    val pp = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        absenceType = "PP",
        absenceSubType = "PP",
        absenceReason = "PC",
      ),
    )
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(pp))

    val res1 = searchTapOccurrences(prisonCode, sort = "absenceType,asc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)
    assertThat(res1.content.map { it.absenceType?.description }).containsExactly(
      pp.absenceType?.description,
      sr.absenceType?.description,
    )

    val res2 = searchTapOccurrences(prisonCode, sort = "absenceType,desc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)
    assertThat(res2.content.map { it.absenceType?.description }).containsExactly(
      sr.absenceType?.description,
      pp.absenceType?.description,
    )

    val res3 = searchTapOccurrences(prisonCode, sort = "absenceReason,asc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res3.content.size).isEqualTo(2)
    assertThat(res3.metadata.totalElements).isEqualTo(2)
    assertThat(res3.content.map { it.absenceReason?.description }).containsExactly(
      sr.absenceReason?.description,
      pp.absenceReason?.description,
    )

    val res4 = searchTapOccurrences(prisonCode, sort = "absenceReason,desc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res4.content.size).isEqualTo(2)
    assertThat(res4.metadata.totalElements).isEqualTo(2)
    assertThat(res4.content.map { it.absenceReason?.description }).containsExactly(
      pp.absenceReason?.description,
      sr.absenceReason?.description,
    )
  }

  @Test
  fun `can sort accompanied by or transport`() {
    val prisonCode = prisonCode()

    val one = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(one))
    val two = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        accompaniedByCode = "GROUP4",
        transportCode = "CAV",
      ),
    )
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(two))

    val res1 = searchTapOccurrences(prisonCode, sort = "accompaniedBy,asc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)
    assertThat(res1.content.map { it.absenceType?.description }).containsExactly(
      two.absenceType?.description,
      one.absenceType?.description,
    )

    val res2 = searchTapOccurrences(prisonCode, sort = "accompaniedBy,desc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)
    assertThat(res2.content.map { it.absenceType?.description }).containsExactly(
      one.absenceType?.description,
      two.absenceType?.description,
    )

    val res3 = searchTapOccurrences(prisonCode, sort = "transport,asc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res3.content.size).isEqualTo(2)
    assertThat(res3.metadata.totalElements).isEqualTo(2)
    assertThat(res3.content.map { it.absenceReason?.description }).containsExactly(
      one.absenceReason?.description,
      two.absenceReason?.description,
    )

    val res4 = searchTapOccurrences(prisonCode, sort = "transport,desc")
      .successResponse<TapOccurrenceSearchResponse>()
    assertThat(res4.content.size).isEqualTo(2)
    assertThat(res4.metadata.totalElements).isEqualTo(2)
    assertThat(res4.content.map { it.absenceReason?.description }).containsExactly(
      two.absenceReason?.description,
      one.absenceReason?.description,
    )
  }

  @Test
  fun `can sort by location`() {
    val prisonCode = prisonCode()

    val authOne = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val one = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authOne,
        location = location(
          description = "A description of the location",
          address = null,
          postcode = null,
          uprn = null,
        ),
      ),
    )
    val authTwo = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val two = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authTwo,
        location = location(description = null, address = "An address", postcode = "H3 4TH"),
      ),
    )
    val authThree = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val three = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authThree,
        location = location(description = "Special Description", address = "Address Line", postcode = postcode()),
      ),
    )

    val res1 = searchTapOccurrences(prisonCode, sort = "location,asc").successResponse<TapOccurrenceSearchResponse>()
    assertThat(res1.content.size).isEqualTo(3)
    assertThat(res1.metadata.totalElements).isEqualTo(3)
    assertThat(res1.content.map { it.location }).containsExactly(one.location, two.location, three.location)

    val res2 = searchTapOccurrences(prisonCode, sort = "location,desc").successResponse<TapOccurrenceSearchResponse>()
    assertThat(res2.content.size).isEqualTo(3)
    assertThat(res2.metadata.totalElements).isEqualTo(3)
    assertThat(res2.content.map { it.location }).containsExactly(three.location, two.location, one.location)
  }

  private fun searchTapOccurrences(
    prisonCode: String,
    start: LocalDate = LocalDate.now(),
    end: LocalDate = LocalDate.now().plusDays(1),
    query: String? = null,
    statuses: List<OccurrenceStatus.Code>? = null,
    sort: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_TAP_OCCURRENCES_URL)
      uri.queryParam("prisonCode", prisonCode)
      uri.queryParam("start", start)
      uri.queryParam("end", end)
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
