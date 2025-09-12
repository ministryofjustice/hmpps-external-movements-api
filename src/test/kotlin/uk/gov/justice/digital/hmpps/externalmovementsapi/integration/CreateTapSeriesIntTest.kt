package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceSeriesOperations.Companion.temporaryAbsenceSeries
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.tapseries.CreateTapSeriesRequest
import java.time.LocalDateTime
import java.util.UUID

class CreateTapSeriesIntTest(
  @Autowired private val tasOperations: TempAbsenceSeriesOperations,
) : IntegrationTest(),
  TempAbsenceSeriesOperations by tasOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .post()
      .uri(CREATE_TAP_SERIES_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    createTapSeries(personIdentifier(), createTapSeriesRequest(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 prisoner does not exist`() {
    val pi = personIdentifier()
    prisonerSearch.getPrisoners("NE1", setOf(pi), listOf())
    val request = createTapSeriesRequest()
    createTapSeries(pi, request).expectStatus().isNotFound
  }

  @Test
  fun `409 conflict - matching tap series already exists`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val series = givenTemporaryAbsenceSeries(temporaryAbsenceSeries(prisonCode, pi))

    val request = createTapSeriesRequest(releaseAt = series.releaseAt, returnBy = series.returnBy)
    createTapSeries(pi, request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `200 ok tap series created successfully`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapSeriesRequest()
    val res = createTapSeries(pi, request)
      .expectStatus().isCreated
      .expectBody<ReferenceId>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceSeries(res.id))
    saved.verifyAgainst(pi, request)
  }

  @Test
  fun `200 ok tap series created successfully using default links`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapSeriesRequest(absenceTypeCode = "PP", absenceSubTypeCode = null, absenceReasonCode = null)
    val res = createTapSeries(pi, request)
      .expectStatus().isCreated
      .expectBody<ReferenceId>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceSeries(res.id))
    saved.verifyAgainst(pi, request.copy(absenceSubTypeCode = "PP", absenceReasonCode = "PC"))
  }

  private fun createTapSeriesRequest(
    submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
    repeat: Boolean = false,
    statusCode: String = "APP-SCH",
    absenceTypeCode: String = "SR",
    absenceSubTypeCode: String? = "RDR",
    absenceReasonCode: String? = "R15",
    releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
    returnBy: LocalDateTime = LocalDateTime.now(),
    accompanied: Boolean = true,
    accompaniedByCode: String? = "L",
    transportCode: String? = "OD",
    notes: String? = "Some notes about the series",
    locationId: String? = UUID.randomUUID().toString(),
    locationTypeCode: String = "CORP",
  ) = CreateTapSeriesRequest(
    submittedAt,
    repeat,
    statusCode,
    absenceTypeCode,
    absenceSubTypeCode,
    absenceReasonCode,
    releaseAt,
    returnBy,
    accompanied,
    accompaniedByCode,
    transportCode,
    notes,
    locationTypeCode,
    locationId,
  )

  private fun createTapSeries(
    personIdentifier: String,
    request: CreateTapSeriesRequest,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .post()
    .uri(CREATE_TAP_SERIES_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val CREATE_TAP_SERIES_URL = "/temporary-absence-series/{personIdentifier}"
  }
}
