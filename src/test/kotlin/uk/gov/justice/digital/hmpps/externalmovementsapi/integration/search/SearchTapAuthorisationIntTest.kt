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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation.Companion.START
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.AbsenceCategorisationFilter
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import java.time.LocalDate

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

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, EXTERNAL_MOVEMENTS_RO])
  fun `403 forbidden without correct role`(role: String) {
    searchTapAuthorisations(
      prisonCode(),
      LocalDate.now(),
      LocalDate.now(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `can find authorisations by date`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now().plusDays(1)
    val end = LocalDate.now().plusDays(3)

    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = start.minusDays(3), end = start.minusDays(1)),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = start.minusDays(3), end = start),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = start, end = end),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = start.minusDays(1), end = end.minusDays(1)),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = start.plusDays(1), end = end.plusDays(1)),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = end, end = end.plusDays(2)),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(prisonCode, start = end.plusDays(1), end = end.plusDays(2)),
    )

    val res = searchTapAuthorisations(prisonCode, start, end).successResponse<TapAuthorisationSearchResponse>()

    assertThat(res.content.size).isEqualTo(5)
    assertThat(res.metadata.totalElements).isEqualTo(5)
  }

  @Test
  fun `can filter authorisations by status`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now().plusDays(1)
    val end = LocalDate.now().plusDays(3)
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.APPROVED,
        start = start,
        end = end.minusDays(1),
      ),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.APPROVED,
        start = start.plusDays(1),
        end = end,
      ),
    )

    listOf(
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          status = AuthorisationStatus.Code.PENDING,
          start = start,
          end = end.minusDays(1),
        ),
      ),
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          status = AuthorisationStatus.Code.PENDING,
          start = start.plusDays(1),
          end = end,
        ),
      ),
    )

    val res = searchTapAuthorisations(prisonCode, start, end, AuthorisationStatus.Code.PENDING)
      .successResponse<TapAuthorisationSearchResponse>()

    assertThat(res.content.size).isEqualTo(2)
    assertThat(res.metadata.totalElements).isEqualTo(2)
  }

  @Test
  fun `can filter by absence categorisation`() {
    val prisonCode = prisonCode()

    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val auth2 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        absenceType = "PP",
        absenceSubType = "PP",
        absenceReasonCategory = null,
        absenceReason = "PC",
        reasonPath = ReasonPath(absenceTypeCode = "PP", absenceSubTypeCode = null, absenceReasonCategoryCode = null, absenceReasonCode = null),
      ),
    )

    val res1 = searchTapAuthorisations(
      prisonCode,
      absenceCategorisation = AbsenceCategorisationFilter(
        ReferenceDataDomain.Code.ABSENCE_TYPE,
        sortedSetOf(auth1.absenceType!!.code),
      ),
    ).successResponse<TapAuthorisationSearchResponse>()

    assertThat(res1.content.size).isEqualTo(1)
    assertThat(res1.metadata.totalElements).isEqualTo(1)
    assertThat(res1.content.first().absenceCategorisation).isEqualTo("Standard ROTL (release on temporary licence) > RDR (resettlement day release) > Paid work > IT and communication")

    val res2 = searchTapAuthorisations(
      prisonCode,
      absenceCategorisation = AbsenceCategorisationFilter(
        ReferenceDataDomain.Code.ABSENCE_SUB_TYPE,
        sortedSetOf(auth1.absenceSubType!!.code),
      ),
    ).successResponse<TapAuthorisationSearchResponse>()

    assertThat(res2.content.size).isEqualTo(1)
    assertThat(res2.metadata.totalElements).isEqualTo(1)
    assertThat(res2.content.first().absenceCategorisation).isEqualTo("Standard ROTL (release on temporary licence) > RDR (resettlement day release) > Paid work > IT and communication")

    val res3 = searchTapAuthorisations(
      prisonCode,
      absenceCategorisation = AbsenceCategorisationFilter(
        ReferenceDataDomain.Code.ABSENCE_REASON,
        sortedSetOf(auth2.absenceReason.code),
      ),
    ).successResponse<TapAuthorisationSearchResponse>()

    assertThat(res3.content.size).isEqualTo(1)
    assertThat(res3.metadata.totalElements).isEqualTo(1)
    assertThat(res3.content.first().absenceCategorisation).isEqualTo("Police production")

    val res4 = searchTapAuthorisations(
      prisonCode,
      absenceCategorisation = AbsenceCategorisationFilter(
        ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY,
        sortedSetOf(auth1.absenceReasonCategory!!.code),
      ),
    ).successResponse<TapAuthorisationSearchResponse>()

    assertThat(res4.content.size).isEqualTo(1)
    assertThat(res4.metadata.totalElements).isEqualTo(1)
    assertThat(res4.content.first().absenceCategorisation).isEqualTo("Standard ROTL (release on temporary licence) > RDR (resettlement day release) > Paid work > IT and communication")
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
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )

    val res = searchTapAuthorisations(prisonCode, query = toFind.person.identifier)
      .successResponse<TapAuthorisationSearchResponse>()

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
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )

    toFind.person.nameFormats().forEach {
      val res = searchTapAuthorisations(prisonCode, query = it)
        .successResponse<TapAuthorisationSearchResponse>()

      assertThat(res.content.size).isEqualTo(1)
      assertThat(res.metadata.totalElements).isEqualTo(1)
    }
  }

  @Test
  fun `can sort by name`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now().plusDays(1)
    val end = LocalDate.now().plusDays(2)

    val p1 = givenPersonSummary(personSummary(lastName = "Smith", firstName = "Jane"))
    val p2 = givenPersonSummary(personSummary(lastName = "Doe", firstName = "John"))
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        p1.identifier,
        start = start,
        end = end,
      ),
    )
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        p2.identifier,
        start = start,
        end = end,
      ),
    )

    val res1 = searchTapAuthorisations(prisonCode, start, end, sort = "firstName,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)

    assertThat(res1.content.map { it.person.personIdentifier }).containsExactly(p1.identifier, p2.identifier)

    val res2 = searchTapAuthorisations(prisonCode, start, end, sort = "lastName,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)

    assertThat(res2.content.map { it.person.personIdentifier }).containsExactly(p2.identifier, p1.identifier)

    val res3 = searchTapAuthorisations(prisonCode, start, end, sort = "firstName,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res3.content.size).isEqualTo(2)
    assertThat(res3.metadata.totalElements).isEqualTo(2)

    assertThat(res3.content.map { it.person.personIdentifier }).containsExactly(p2.identifier, p1.identifier)

    val res4 = searchTapAuthorisations(prisonCode, start, end, sort = "lastName,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res4.content.size).isEqualTo(2)
    assertThat(res4.metadata.totalElements).isEqualTo(2)

    assertThat(res4.content.map { it.person.personIdentifier }).containsExactly(p1.identifier, p2.identifier)
  }

  @Test
  fun `can sort by status`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now().plusDays(1)
    val end = LocalDate.now().plusDays(2)

    val pending = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        start = start,
        end = end,
      ),
    )
    val cancelled = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.CANCELLED,
        start = start,
        end = end,
      ),
    )
    val denied = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.DENIED,
        start = start,
        end = end,
      ),
    )
    val approved = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.APPROVED,
        start = start,
        end = end,
      ),
    )

    val res1 = searchTapAuthorisations(prisonCode, sort = "status,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res1.content.size).isEqualTo(4)
    assertThat(res1.metadata.totalElements).isEqualTo(4)

    assertThat(res1.content.map { it.person.personIdentifier }).containsExactly(
      pending.person.identifier,
      approved.person.identifier,
      denied.person.identifier,
      cancelled.person.identifier,
    )

    val res2 = searchTapAuthorisations(prisonCode, sort = "status,desc")
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

  @Test
  fun `can sort type or reason`() {
    val prisonCode = prisonCode()
    val start = LocalDate.now()
    val end = LocalDate.now()

    val sr = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        start = start,
        end = end,
      ),
    )
    val pp = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        absenceType = "PP",
        absenceSubType = "PP",
        absenceReason = "PC",
        start = start,
        end = end,
      ),
    )

    val res1 = searchTapAuthorisations(prisonCode, start, end, sort = "absenceType,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)
    assertThat(res1.content.map { it.absenceType?.description }).containsExactly(
      pp.absenceType?.description,
      sr.absenceType?.description,
    )

    val res2 = searchTapAuthorisations(prisonCode, start, end, sort = "absenceType,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)
    assertThat(res2.content.map { it.absenceType?.description }).containsExactly(
      sr.absenceType?.description,
      pp.absenceType?.description,
    )

    val res3 = searchTapAuthorisations(prisonCode, start, end, sort = "absenceReason,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res3.content.size).isEqualTo(2)
    assertThat(res3.metadata.totalElements).isEqualTo(2)
    assertThat(res3.content.map { it.absenceReason?.description }).containsExactly(
      sr.absenceReason.description,
      pp.absenceReason.description,
    )

    val res4 = searchTapAuthorisations(prisonCode, start, end, sort = "absenceReason,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res4.content.size).isEqualTo(2)
    assertThat(res4.metadata.totalElements).isEqualTo(2)
    assertThat(res4.content.map { it.absenceReason?.description }).containsExactly(
      pp.absenceReason.description,
      sr.absenceReason.description,
    )
  }

  @Test
  fun `can sort by repeat or single`() {
    val prisonCode = prisonCode()

    val repeat = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, repeat = true))
    val single = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))

    val res1 = searchTapAuthorisations(prisonCode, sort = "repeat,asc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res1.content.size).isEqualTo(2)
    assertThat(res1.metadata.totalElements).isEqualTo(2)
    assertThat(res1.content.map { it.absenceType?.description }).containsExactly(
      repeat.absenceType?.description,
      single.absenceType?.description,
    )

    val res2 = searchTapAuthorisations(prisonCode, sort = "repeat,desc")
      .successResponse<TapAuthorisationSearchResponse>()
    assertThat(res2.content.size).isEqualTo(2)
    assertThat(res2.metadata.totalElements).isEqualTo(2)
    assertThat(res2.content.map { it.absenceType?.description }).containsExactly(
      single.absenceType?.description,
      repeat.absenceType?.description,
    )
  }

  private fun searchTapAuthorisations(
    prisonCode: String,
    start: LocalDate = LocalDate.now(),
    end: LocalDate = LocalDate.now().plusDays(1),
    status: AuthorisationStatus.Code? = null,
    absenceCategorisation: AbsenceCategorisationFilter? = null,
    query: String? = null,
    sort: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .post()
    .uri(SEARCH_TAP_AUTH_URL)
    .bodyValue(TapAuthorisationSearchRequest(prisonCode, start, end, setOfNotNull(status), absenceCategorisation, query, sort = sort ?: START))
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_TAP_AUTH_URL = "/search/temporary-absence-authorisations"
  }
}
