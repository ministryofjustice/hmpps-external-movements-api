package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDenied
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
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
      .post()
      .uri(TAP_AUTHORISATION_MODIFICATION_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    approveAuthorisation(
      newUuid(),
      denyAuthorisationRequest(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    approveAuthorisation(newUuid(), denyAuthorisationRequest()).expectStatus().isNotFound
  }

  @ParameterizedTest
  @EnumSource(TapAuthorisationStatus.Code::class, mode = EXCLUDE, names = ["PENDING", "DENIED"])
  fun `409 - authorisation not awaiting approval cannot be denied`(status: TapAuthorisationStatus.Code) {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = status))
    val res = approveAuthorisation(auth.id, denyAuthorisationRequest()).errorResponse(HttpStatus.CONFLICT)
    assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(res.userMessage).isEqualTo("Temporary absence authorisation not awaiting approval")
  }

  @Test
  fun `200 ok - authorisation denied`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = denyAuthorisationRequest()
    approveAuthorisation(auth.id, request).expectStatus().isOk

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.status.code).isEqualTo(DENIED.name)
    val absence = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(absence.status.code).isEqualTo(TapOccurrenceStatus.Code.DENIED.name)

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
      setOf(TemporaryAbsenceAuthorisationDenied(auth.person.identifier, auth.id)),
    )
  }

  private fun denyAuthorisationRequest(
    reason: String? = "Evidence justifying the denial",
  ) = DenyAuthorisation(reason)

  private fun approveAuthorisation(
    id: UUID,
    request: DenyAuthorisation,
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
