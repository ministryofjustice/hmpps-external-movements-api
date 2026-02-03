package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.occurrence

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceComments
import java.util.UUID

class ChangeOccurrenceCommentsIntTest(
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

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    applyOccurrenceComments(
      newUuid(),
      action(),
      role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 occurrence does not exist`() {
    applyOccurrenceComments(newUuid(), action()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok single tap occurrence comments updated successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action()
    val res = applyOccurrenceComments(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceCommentsChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(AuditedAction.Change("comments", occurrence.comments, request.comments))

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.comments).isEqualTo(request.comments)
    assertThat(saved.authorisation.comments).isEqualTo(request.comments)

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

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceCommentsChanged(occurrence.authorisation.person.identifier, occurrence.id).publication(occurrence.id),
        TemporaryAbsenceAuthorisationCommentsChanged(auth.person.identifier, auth.id).publication(auth.id),
      ),
    )
  }

  @Test
  fun `200 ok single tap occurrence comments updated - no publish`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = AuthorisationStatus.Code.PENDING))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.PENDING.name)

    val request = action()
    val res = applyOccurrenceComments(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceCommentsChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(AuditedAction.Change("comments", occurrence.comments, request.comments))

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.comments).isEqualTo(request.comments)
    assertThat(saved.authorisation.comments).isEqualTo(request.comments)

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

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceCommentsChanged(occurrence.authorisation.person.identifier, occurrence.id).publication(occurrence.id) { false },
        TemporaryAbsenceAuthorisationCommentsChanged(auth.person.identifier, auth.id).publication(auth.id),
      ),
    )
  }

  @Test
  fun `200 ok repeat tap occurrence comments updated successfully`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = true))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action()
    val res = applyOccurrenceComments(occurrence.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceCommentsChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(AuditedAction.Change("comments", occurrence.comments, request.comments))

    val saved = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(saved.comments).isEqualTo(request.comments)
    assertThat(saved.authorisation.comments).isEqualTo(auth.comments)

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
        TemporaryAbsenceCommentsChanged(occurrence.authorisation.person.identifier, occurrence.id),
      ),
    )
  }

  private fun action(
    comments: String = (0..10).joinToString(separator = " ") { word(6) },
    reason: String? = (0..5).joinToString(separator = " ") { word(4) },
  ) = ChangeOccurrenceComments(comments, reason)

  private fun applyOccurrenceComments(
    id: UUID,
    request: ChangeOccurrenceComments,
    role: String? = Roles.TEMPORARY_ABSENCE_RW,
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
