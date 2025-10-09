package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CreateTapAuthorisationIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

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
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))

    val request = createTapAuthorisationRequest(
      occurrences = listOf(
        createTapOccurrenceRequest(
          releaseAt = occurrence.releaseAt,
          returnBy = occurrence.returnBy,
        ),
      ),
    )
    createTapAuthorisation(pi, request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `200 ok tap authorisation created successfully`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapAuthorisationRequest()
    val res = createTapAuthorisation(pi, request).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.approvedAt).isNull()
    assertThat(saved.approvedBy).isNull()
    val occurrences = findForAuthorisation(saved.id)
    occurrences.first().verifyAgainst(pi, request.occurrences.first(), request)
  }

  @Test
  fun `200 ok tap authorisation created successfully using default links`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request =
      createTapAuthorisationRequest(
        absenceTypeCode = "PP",
        absenceSubTypeCode = null,
        absenceReasonCode = null,
        statusCode = TapAuthorisationStatus.Code.APPROVED,
      )
    val res = createTapAuthorisation(pi, request).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request.copy(absenceSubTypeCode = "PP", absenceReasonCode = "PC"))
    assertThat(saved.approvedAt).isNotNull()
    assertThat(saved.approvedBy).isNotNull()
    val occurrences = findForAuthorisation(saved.id)
    occurrences.first().verifyAgainst(pi, request.occurrences.first(), request)
  }

  private fun createTapOccurrenceRequest(
    releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
    returnBy: LocalDateTime = LocalDateTime.now(),
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    notes: String? = "Some notes about the authorisation",
    locationId: String = UUID.randomUUID().toString(),
    locationTypeCode: String = "CORP",
  ) = CreateTapOccurrenceRequest(
    releaseAt = releaseAt,
    returnBy = returnBy,
    accompaniedByCode = accompaniedByCode,
    transportCode = transportCode,
    notes = notes,
    locationTypeCode = locationTypeCode,
    locationId = locationId,
  )

  private fun createTapAuthorisationRequest(
    submittedAt: LocalDateTime = LocalDateTime.now().minusMonths(1),
    absenceTypeCode: String = "SR",
    absenceSubTypeCode: String? = "RDR",
    absenceReasonCode: String? = "R15",
    fromDate: LocalDate = LocalDate.now().minusDays(7),
    toDate: LocalDate = LocalDate.now().minusDays(1),
    statusCode: TapAuthorisationStatus.Code = TapAuthorisationStatus.Code.PENDING,
    occurrences: List<CreateTapOccurrenceRequest> = listOf(createTapOccurrenceRequest()),
    notes: String? = null,
    repeat: Boolean = false,
  ) = CreateTapAuthorisationRequest(
    submittedAt,
    absenceTypeCode,
    absenceSubTypeCode,
    absenceReasonCode,
    occurrences,
    statusCode,
    notes,
    repeat,
    fromDate,
    toDate,
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
    const val CREATE_TAP_AUTH_URL = "/temporary-absence-authorisations/{personIdentifier}"
  }
}
