package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationAccompaniment
import java.time.LocalDateTime
import java.util.UUID

class ChangeAuthorisationAccompanimentIntTest(
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

  @Test
  fun `403 forbidden without correct role`() {
    applyAccompaniment(
      UUID.randomUUID(),
      action(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    applyAccompaniment(newUuid(), action()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok - authorisation accompaniment changed`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val prev = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusDays(3),
        end = LocalDateTime.now().minusDays(2),
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action()
    val res = applyAccompaniment(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(
      TemporaryAbsenceAuthorisationAccompanimentChanged.EVENT_TYPE,
      TemporaryAbsenceAccompanimentChanged.EVENT_TYPE,
    )
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("accompaniedBy", "Prison officer escort (local)", "Unaccompanied"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.accompaniedBy.code).isEqualTo(request.accompaniedByCode)

    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
    assertThat(occurrence.accompaniedBy.code).isEqualTo(request.accompaniedByCode)

    val previous = requireNotNull(findTemporaryAbsenceOccurrence(prev.id))
    assertThat(previous.accompaniedBy.code).isEqualTo(prev.accompaniedBy.code)

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
        TemporaryAbsenceAuthorisationAccompanimentChanged(auth.person.identifier, auth.id),
        TemporaryAbsenceAccompanimentChanged(auth.person.identifier, occ.id),
      ),
    )
  }

  @Test
  fun `200 ok - no-op change accompaniment request`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val request = action(auth.accompaniedBy.code)
    val res = applyAccompaniment(auth.id, request).successResponse<AuditHistory>()
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
    accompaniedByCode: String = "U",
    reason: String? = (0..5).joinToString(separator = " ") { word(4) },
  ) = ChangeAuthorisationAccompaniment(accompaniedByCode, reason)

  private fun applyAccompaniment(
    id: UUID,
    request: ChangeAuthorisationAccompaniment,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
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
