package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.PENDING
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import java.time.LocalDateTime
import java.util.UUID

class ChangeAuthorisationCommentsIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TAP_AUTHORISATION_MODIFICATION_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    applyComments(
      UUID.randomUUID(),
      action(),
      role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    applyComments(newUuid(), action()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok - authorisation comments changed`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val prev = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusDays(1),
        end = LocalDateTime.now().minusHours(1),
        comments = "Previous comments on a past occurrence",
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action()
    val res = applyComments(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationCommentsChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("comments", auth.comments, request.comments),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
    assertThat(occurrence.absenceReason?.code).isEqualTo(saved.absenceReason?.code)
    assertThat(occurrence.absenceReasonCategory?.code).isEqualTo(saved.absenceReasonCategory?.code)
    assertThat(occurrence.absenceSubType?.code).isEqualTo(saved.absenceSubType?.code)
    assertThat(occurrence.absenceType?.code).isEqualTo(saved.absenceType?.code)

    val previous = requireNotNull(findTemporaryAbsenceOccurrence(prev.id))
    assertThat(previous.comments).isEqualTo(prev.comments)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationCommentsChanged(auth.person.identifier, auth.id),
        TemporaryAbsenceCommentsChanged(auth.person.identifier, occ.id),
      ),
    )
  }

  @Test
  fun `200 ok - no-op change comments request`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val request = action(auth.comments!!)
    val res = applyComments(auth.id, request).successResponse<AuditHistory>()
    assertThat(res.content).isEmpty()

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get(),
    )
  }

  private fun action(
    comments: String = (0..10).joinToString(separator = " ") { word(6) },
    reason: String? = (0..5).joinToString(separator = " ") { word(4) },
  ) = ChangeAuthorisationComments(comments, reason)

  private fun applyComments(
    id: UUID,
    request: ChangeAuthorisationComments,
    role: String? = Roles.TEMPORARY_ABSENCE_RW,
  ) = webTestClient
    .put()
    .uri(TAP_AUTHORISATION_MODIFICATION_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val TAP_AUTHORISATION_MODIFICATION_URL = "/temporary-absence-authorisations/{id}"
  }
}
