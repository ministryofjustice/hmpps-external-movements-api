package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDenied
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import java.util.UUID

class DenyTapAuthorisationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TAP_AUTHORISATION_MODIFICATION_URL, IdGenerator.newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    denyAuthorisation(
      IdGenerator.newUuid(),
      denyAuthorisationRequest(),
      role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    denyAuthorisation(IdGenerator.newUuid(), denyAuthorisationRequest()).expectStatus().isNotFound
  }

  @ParameterizedTest
  @EnumSource(TapAuthorisationStatus.Code::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING", "DENIED"])
  fun `409 - authorisation not awaiting approval cannot be denied`(status: TapAuthorisationStatus.Code) {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation(
        status = status,
      ),
    )
    val res = denyAuthorisation(auth.id, denyAuthorisationRequest()).errorResponse(HttpStatus.CONFLICT)
    Assertions.assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    Assertions.assertThat(res.userMessage).isEqualTo("Temporary absence authorisation not awaiting approval")
  }

  @Test
  fun `200 ok - authorisation denied`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation(
        status = TapAuthorisationStatus.Code.PENDING,
      ),
    )
    val occurrence = givenTemporaryAbsenceOccurrence(
      TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence(
        auth,
      ),
    )
    val request = denyAuthorisationRequest()
    val res = denyAuthorisation(auth.id, request).successResponse<AuditHistory>().content.single()
    Assertions.assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationDenied.Companion.EVENT_TYPE)
    Assertions.assertThat(res.reason).isEqualTo(request.reason)
    Assertions.assertThat(res.changes).containsExactly(
      AuditedAction.Change("status", "To be reviewed", "Denied"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    Assertions.assertThat(saved.status.code).isEqualTo(TapAuthorisationStatus.Code.DENIED.name)
    val absence = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    Assertions.assertThat(absence.status.code).isEqualTo(TapOccurrenceStatus.Code.DENIED.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.Companion.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(
      saved,
      setOf(TemporaryAbsenceAuthorisationDenied.Companion(auth.person.identifier, auth.id)),
    )
  }

  private fun denyAuthorisationRequest(
    reason: String? = "Evidence justifying the denial",
  ) = DenyAuthorisation(reason)

  private fun denyAuthorisation(
    id: UUID,
    request: DenyAuthorisation,
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
