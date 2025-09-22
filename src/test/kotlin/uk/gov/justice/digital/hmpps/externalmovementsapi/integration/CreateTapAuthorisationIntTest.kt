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
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import java.time.LocalDateTime
import java.util.UUID

class CreateTapAuthorisationIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .post()
      .uri(CREATE_TAP_AUTH_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    createTapAuthorisation(
      personIdentifier(),
      createTapAuthorisationRequest(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 prisoner does not exist`() {
    val pi = personIdentifier()
    prisonerSearch.getPrisoners("NE1", setOf(pi), listOf())
    val request = createTapAuthorisationRequest()
    createTapAuthorisation(pi, request).expectStatus().isNotFound
  }

  @Test
  fun `409 conflict - matching tap authorisation already exists`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, pi))

    val request = createTapAuthorisationRequest(releaseAt = authorisation.releaseAt, returnBy = authorisation.returnBy)
    createTapAuthorisation(pi, request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `200 ok tap authorisation created successfully`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapAuthorisationRequest()
    val res = createTapAuthorisation(pi, request)
      .expectStatus().isCreated
      .expectBody<ReferenceId>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
  }

  @Test
  fun `200 ok tap authorisation created successfully using default links`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request =
      createTapAuthorisationRequest(absenceTypeCode = "PP", absenceSubTypeCode = null, absenceReasonCode = null)
    val res = createTapAuthorisation(pi, request)
      .expectStatus().isCreated
      .expectBody<ReferenceId>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request.copy(absenceSubTypeCode = "PP", absenceReasonCode = "PC"))
  }

  private fun createTapAuthorisationRequest(
    submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
    repeat: Boolean = false,
    approvalRequired: Boolean = true,
    absenceTypeCode: String = "SR",
    absenceSubTypeCode: String? = "RDR",
    absenceReasonCode: String? = "R15",
    releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
    returnBy: LocalDateTime = LocalDateTime.now(),
    accompanied: Boolean = true,
    accompaniedByCode: String? = "L",
    transportCode: String? = "OD",
    notes: String? = "Some notes about the authorisation",
    locationId: String? = UUID.randomUUID().toString(),
    locationTypeCode: String = "CORP",
  ) = CreateTapAuthorisationRequest(
    submittedAt,
    repeat,
    approvalRequired,
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

  private fun createTapAuthorisation(
    personIdentifier: String,
    request: CreateTapAuthorisationRequest,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .post()
    .uri(CREATE_TAP_AUTH_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val CREATE_TAP_AUTH_URL = "/temporary-absence-authorisation/{personIdentifier}"
  }
}
