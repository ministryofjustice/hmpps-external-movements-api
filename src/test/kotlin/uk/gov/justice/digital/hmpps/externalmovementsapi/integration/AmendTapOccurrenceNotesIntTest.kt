package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceNotesChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.AmendOccurrenceNotes
import java.util.UUID

class AmendTapOccurrenceNotesIntTest(
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
    amendOccurrenceNotes(
      newUuid(),
      amendNotesRequest(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 occurrence does not exist`() {
    amendOccurrenceNotes(newUuid(), amendNotesRequest()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok tap occurrence rescheduled successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = amendNotesRequest()
    val res = amendOccurrenceNotes(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceNotesChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(AuditedAction.Change("notes", occurrence.notes, request.notes))

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.notes).isEqualTo(request.notes)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceNotesChanged(occurrence.authorisation.person.identifier, occurrence.id)))
  }

  private fun amendNotesRequest(
    notes: String = (0..10).joinToString(separator = " ") { word(6) },
    reason: String? = (0..5).joinToString(separator = " ") { word(4) },
  ) = AmendOccurrenceNotes(notes, reason)

  private fun amendOccurrenceNotes(
    id: UUID,
    request: AmendOccurrenceNotes,
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
