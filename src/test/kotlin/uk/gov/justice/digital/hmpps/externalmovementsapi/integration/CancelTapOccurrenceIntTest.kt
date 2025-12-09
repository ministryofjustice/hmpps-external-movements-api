package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import java.time.LocalDateTime
import java.util.UUID

class CancelTapOccurrenceIntTest(
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
    cancelOccurrence(
      newUuid(),
      cancelOccurrenceRequest(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 occurrence does not exist`() {
    cancelOccurrence(newUuid(), cancelOccurrenceRequest()).expectStatus().isNotFound
  }

  @Test
  fun `409 - occurrence not scheduled cannot be cancelled`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, returnBy = LocalDateTime.now().minusHours(4)))
    val res = cancelOccurrence(occurrence.id, cancelOccurrenceRequest()).errorResponse(HttpStatus.CONFLICT)
    assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(res.userMessage).isEqualTo("Temporary absence not currently scheduled")
  }

  @Test
  fun `200 ok tap occurrence cancelled for single authorisation successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = cancelOccurrenceRequest()
    val res = cancelOccurrence(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactlyInAnyOrder(
      TemporaryAbsenceCancelled.EVENT_TYPE,
      TemporaryAbsenceAuthorisationCancelled.EVENT_TYPE,
    )
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).isEmpty()

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.status.code).isEqualTo(TapOccurrenceStatus.Code.CANCELLED.name)
    assertThat(saved.authorisation.status.code).isEqualTo(TapAuthorisationStatus.Code.CANCELLED.name)

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
        TemporaryAbsenceCancelled(occurrence.authorisation.person.identifier, occurrence.id),
        TemporaryAbsenceAuthorisationCancelled(auth.person.identifier, auth.id),
      ),
    )
  }

  @Test
  fun `200 ok tap occurrence cancelled for repeat authorisation successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = true))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = cancelOccurrenceRequest()
    val res = cancelOccurrence(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactlyInAnyOrder(TemporaryAbsenceCancelled.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).isEmpty()

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.status.code).isEqualTo(TapOccurrenceStatus.Code.CANCELLED.name)
    assertThat(saved.authorisation.status.code).isEqualTo(auth.status.code)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(
      saved,
      setOf(
        TemporaryAbsenceCancelled(occurrence.authorisation.person.identifier, occurrence.id),
      ),
    )
  }

  private fun cancelOccurrenceRequest(
    reason: String? = "A reason for the cancellation",
  ) = CancelOccurrence(reason)

  private fun cancelOccurrence(
    id: UUID,
    request: CancelOccurrence,
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
