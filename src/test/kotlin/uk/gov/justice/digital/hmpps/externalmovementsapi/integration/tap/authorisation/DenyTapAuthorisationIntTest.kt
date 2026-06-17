package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDenied
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceDenied
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.AuthorisationActions
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.TapAuthorisationModifications.Companion.NOT_AWAITING_APPROVAL
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
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_RW])
  fun `403 forbidden without correct role`(role: String) {
    denyAuthorisation(
      IdGenerator.newUuid(),
      DenyAuthorisation(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    denyAuthorisation(IdGenerator.newUuid(), DenyAuthorisation()).expectStatus().isNotFound
  }

  @ParameterizedTest
  @EnumSource(AuthorisationStatus.Code::class, mode = EnumSource.Mode.EXCLUDE, names = ["PENDING", "DENIED"])
  fun `409 - authorisation not awaiting approval cannot be denied`(status: AuthorisationStatus.Code) {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        status = status,
      ),
    )
    val res = denyAuthorisation(auth.id, DenyAuthorisation()).errorResponse(HttpStatus.CONFLICT)
    assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(res.developerMessage).isEqualTo(NOT_AWAITING_APPROVAL)
  }

  @Test
  fun `200 ok - authorisation denied`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.PENDING,
      ),
    )
    val occurrence = givenTemporaryAbsenceOccurrence(
      TempAbsenceOccurrenceOperations.temporaryAbsenceOccurrence(
        auth,
      ),
    )
    val request = DenyAuthorisation()
    val reason = word(26)
    val res = denyAuthorisation(auth.id, request, reason).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationDenied.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("status", "To be reviewed", "Denied"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.DENIED.name)
    val absence = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(absence.status.code).isEqualTo(OccurrenceStatus.Code.DENIED.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = reason),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationDenied(auth.person.identifier, auth.id).publication(auth.id),
        TemporaryAbsenceDenied(auth.person.identifier, occurrence.id).publication(occurrence.id) { false },
      ),
    )
  }

  private fun denyAuthorisation(
    id: UUID,
    request: DenyAuthorisation,
    reason: String? = word(20),
    role: String? = EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .put()
    .uri(TAP_AUTHORISATION_MODIFICATION_URL, id)
    .bodyValue(AuthorisationActions(listOf(request), reason))
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val TAP_AUTHORISATION_MODIFICATION_URL = "/temporary-absence-authorisations/{id}"
  }
}
