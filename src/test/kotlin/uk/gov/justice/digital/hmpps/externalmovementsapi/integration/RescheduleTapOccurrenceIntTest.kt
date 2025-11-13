package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.CreateTapAuthorisationIntTest.Companion.CREATE_TAP_AUTH_URL
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class RescheduleTapOccurrenceIntTest(
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
    rescheduleOccurrence(
      newUuid(),
      rescheduleOccurrenceRequest(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 occurrence does not exist`() {
    rescheduleOccurrence(newUuid(), rescheduleOccurrenceRequest()).expectStatus().isNotFound
  }

  @Test
  fun `400 at least one date must be provided`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = rescheduleOccurrenceRequest(null, null)
    val res = rescheduleOccurrence(occurrence.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Either release or return date must be provided.")
  }

  @Test
  fun `200 ok tap occurrence rescheduled successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = rescheduleOccurrenceRequest(LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(8))
    rescheduleOccurrence(occurrence.id, request).expectStatus().isOk

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.releaseAt).isCloseTo(request.releaseAt, within(2, ChronoUnit.SECONDS))
    assertThat(saved.returnBy).isCloseTo(request.returnBy, within(2, ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceRescheduled(occurrence.authorisation.personIdentifier, occurrence.id)))
  }

  private fun rescheduleOccurrenceRequest(
    releaseAt: LocalDateTime? = LocalDateTime.now().minusHours(2),
    returnBy: LocalDateTime? = LocalDateTime.now().plusHours(2),
    reason: String? = "A reason for the reschedule",
  ) = RescheduleOccurrence(releaseAt, returnBy, reason)

  private fun rescheduleOccurrence(
    id: UUID,
    request: RescheduleOccurrence,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .put()
    .uri(RESCHEDULE_OCCURRENCE_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RESCHEDULE_OCCURRENCE_URL = "/temporary-absence-occurrences/{id}"
  }
}
