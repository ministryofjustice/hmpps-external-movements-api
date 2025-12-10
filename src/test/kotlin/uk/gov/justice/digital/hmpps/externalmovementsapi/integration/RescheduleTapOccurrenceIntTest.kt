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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class RescheduleTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TAP_OCCURRENCE_MODIFICATION_URL, newUuid())
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
    assertThat(res.userMessage).isEqualTo("Validation failure: Either start or end must be provided.")
  }

  @Test
  fun `400 if absence is outside of repeat authorisation date range`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        start = LocalDate.now(),
        end = LocalDate.now().plusDays(3),
        repeat = true,
      ),
    )
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(1),
        end = LocalDateTime.now().plusDays(2),
      ),
    )
    val request = rescheduleOccurrenceRequest(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4))
    val res = rescheduleOccurrence(occurrence.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Temporary absence must be within the authorised date range.")
  }

  @Test
  fun `200 ok repeat tap occurrence rescheduled successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = true))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = rescheduleOccurrenceRequest(LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4))
    val res = rescheduleOccurrence(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceRescheduled.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes.map { it.propertyName }).containsExactly("start", "end")

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.start).isCloseTo(request.start, within(2, ChronoUnit.SECONDS))
    assertThat(saved.end).isCloseTo(request.end, within(2, ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceRescheduled(occurrence.authorisation.person.identifier, occurrence.id)))
  }

  @Test
  fun `200 ok single tap occurrence rescheduled successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = false))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = rescheduleOccurrenceRequest(LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(8))
    val res = rescheduleOccurrence(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactlyInAnyOrder(
      TemporaryAbsenceRescheduled.EVENT_TYPE,
      TemporaryAbsenceAuthorisationDateRangeChanged.EVENT_TYPE,
    )
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes.map { it.propertyName }).containsExactly("start", "end")

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.start).isCloseTo(request.start, within(2, ChronoUnit.SECONDS))
    assertThat(saved.end).isCloseTo(request.end, within(2, ChronoUnit.SECONDS))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(
      saved,
      setOf(
        TemporaryAbsenceRescheduled(occurrence.authorisation.person.identifier, occurrence.id),
        TemporaryAbsenceAuthorisationDateRangeChanged(auth.person.identifier, auth.id),
      ),
    )
  }

  private fun rescheduleOccurrenceRequest(
    start: LocalDateTime? = LocalDateTime.now().minusHours(2),
    end: LocalDateTime? = LocalDateTime.now().plusHours(2),
    reason: String? = "A reason for the reschedule",
  ) = RescheduleOccurrence(start, end, reason)

  private fun rescheduleOccurrence(
    id: UUID,
    request: RescheduleOccurrence,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .put()
    .uri(TAP_OCCURRENCE_MODIFICATION_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val TAP_OCCURRENCE_MODIFICATION_URL = "/temporary-absence-occurrences/{id}"
  }
}
