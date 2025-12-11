package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.occurrence

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CreateTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .post()
      .uri(CREATE_OCCURRENCE_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    createOccurrence(
      UUID.randomUUID(),
      request(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    createOccurrence(newUuid(), request()).expectStatus().isNotFound
  }

  @ParameterizedTest
  @MethodSource("validationRequests")
  fun `400 bad request - validation exceptions`(request: CreateOccurrenceRequest, message: String) {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val response = createOccurrence(authorisation.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(response.userMessage).isEqualTo(message)
  }

  @Test
  fun `200 ok - an occurrence is added to an authorisation with independent comments`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = request()
    val response = createOccurrence(authorisation.id, request).successResponse<ReferenceId>()

    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(response.id))
    occurrence.verifyAgainst(request)
    occurrence.verifyAgainst(authorisation)
  }

  @Test
  fun `200 ok - an occurrence is added to an authorisation without comments`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = request(comments = null)
    val response = createOccurrence(authorisation.id, request).successResponse<ReferenceId>()

    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(response.id))
    assertThat(occurrence.authorisation.id).isEqualTo(authorisation.id)
    occurrence.verifyAgainst(request)
    occurrence.verifyAgainst(authorisation)
  }

  private fun TemporaryAbsenceOccurrence.verifyAgainst(request: CreateOccurrenceRequest) {
    assertThat(start).isCloseTo(request.start, within(1, ChronoUnit.SECONDS))
    assertThat(end).isCloseTo(request.end, within(1, ChronoUnit.SECONDS))
    assertThat(location).isEqualTo(request.location)
    assertThat(comments).isEqualTo(request.comments ?: authorisation.comments)
  }

  private fun TemporaryAbsenceOccurrence.verifyAgainst(authorisation: TemporaryAbsenceAuthorisation) {
    assertThat(absenceType?.code).isEqualTo(authorisation.absenceType?.code)
    assertThat(absenceSubType?.code).isEqualTo(authorisation.absenceSubType?.code)
    assertThat(absenceReasonCategory?.code).isEqualTo(authorisation.absenceReasonCategory?.code)
    assertThat(absenceReason?.code).isEqualTo(authorisation.absenceReason?.code)
    assertThat(accompaniedBy.code).isEqualTo(authorisation.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(authorisation.transport.code)
  }

  private fun createOccurrence(
    id: UUID,
    request: CreateOccurrenceRequest,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .post()
    .uri(CREATE_OCCURRENCE_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val CREATE_OCCURRENCE_URL = "/temporary-absence-authorisations/{id}/occurrences"

    private fun request(
      start: LocalDateTime = LocalDateTime.now().plusDays(1),
      end: LocalDateTime = LocalDateTime.now().plusDays(2),
      location: Location = location(),
      comments: String? = word(20),
    ) = CreateOccurrenceRequest(start, end, location, comments)

    @JvmStatic
    fun validationRequests() = listOf(
      Arguments.of(
        request(start = LocalDateTime.now().minusDays(5), end = LocalDateTime.now().minusDays(4)),
        "Validation failure: Absence cannot be scheduled in the past.",
      ),
      Arguments.of(
        request(start = LocalDateTime.now().plusDays(5), end = LocalDateTime.now().plusDays(4)),
        "Validation failure: End must be after start.",
      ),
      Arguments.of(
        request(location = location(description = null, address = null, postcode = null)),
        "Validation failure: Either a description or partial address must be specified.",
      ),
      Arguments.of(
        request(start = LocalDateTime.now().plusDays(7), end = LocalDateTime.now().plusDays(8)),
        "Validation failure: Temporary absence must be within the authorised date range.",
      ),
    )
  }
}
