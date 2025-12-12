package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import java.util.UUID

class RecategoriseAuthorisationIntTest(
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
    recategoriseAuthorisation(
      UUID.randomUUID(),
      action(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    recategoriseAuthorisation(newUuid(), action()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok - authorisation recategorised`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action("PP", null, null, null)
    val res = recategoriseAuthorisation(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationRecategorised.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("absenceType", "Standard ROTL (Release on Temporary Licence)", "Police production"),
      AuditedAction.Change("absenceSubType", "RDR (Resettlement Day Release)", "Police production"),
      AuditedAction.Change("absenceReasonCategory", "Paid work", null),
      AuditedAction.Change("absenceReason", "IT and communication", "Police Production"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
    assertThat(occurrence.absenceReason?.code).isEqualTo(saved.absenceReason?.code)
    assertThat(occurrence.absenceReasonCategory?.code).isEqualTo(saved.absenceReasonCategory?.code)
    assertThat(occurrence.absenceSubType?.code).isEqualTo(saved.absenceSubType?.code)
    assertThat(occurrence.absenceType?.code).isEqualTo(saved.absenceType?.code)

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
        TemporaryAbsenceAuthorisationRecategorised(auth.person.identifier, auth.id),
        TemporaryAbsenceRecategorised(auth.person.identifier, occ.id),
      ),
    )
  }

  @Test
  fun `200 ok - authorisation recategorised type and reason`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = action("SE", null, null, "C6")
    val res = recategoriseAuthorisation(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationRecategorised.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("absenceType", "Standard ROTL (Release on Temporary Licence)", "Security escort"),
      AuditedAction.Change("absenceSubType", "RDR (Resettlement Day Release)", null),
      AuditedAction.Change("absenceReasonCategory", "Paid work", null),
      AuditedAction.Change("absenceReason", "IT and communication", "Inpatient medical or dental appointment"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
    assertThat(occurrence.absenceReason?.code).isEqualTo(saved.absenceReason?.code)
    assertThat(occurrence.absenceReasonCategory?.code).isEqualTo(saved.absenceReasonCategory?.code)
    assertThat(occurrence.absenceSubType?.code).isEqualTo(saved.absenceSubType?.code)
    assertThat(occurrence.absenceType?.code).isEqualTo(saved.absenceType?.code)

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
        TemporaryAbsenceAuthorisationRecategorised(auth.person.identifier, auth.id),
        TemporaryAbsenceRecategorised(auth.person.identifier, occ.id),
      ),
    )
  }

  @Test
  fun `200 ok - no-op re-categorisation request`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val request = action(auth.absenceType?.code, auth.absenceSubType?.code, auth.absenceReasonCategory?.code, auth.absenceReason?.code)
    val res = recategoriseAuthorisation(auth.id, request).successResponse<AuditHistory>()
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
    absenceTypeCode: String? = "SR",
    absenceSubTypeCode: String? = "RDR",
    absenceReasonCategoryCode: String? = "PW",
    absenceReasonCode: String? = "R15",
    reason: String? = "Reason for changing the categorisation",
  ) = RecategoriseAuthorisation(
    absenceTypeCode,
    absenceSubTypeCode,
    absenceReasonCategoryCode,
    absenceReasonCode,
    reason = reason,
  )

  private fun recategoriseAuthorisation(
    id: UUID,
    request: RecategoriseAuthorisation,
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
