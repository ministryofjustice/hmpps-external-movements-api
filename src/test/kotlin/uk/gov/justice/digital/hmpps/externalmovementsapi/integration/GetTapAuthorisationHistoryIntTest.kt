package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersServer.Companion.user
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import java.util.UUID

class GetTapAuthorisationHistoryIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_TAP_AUTHORISATION_HISTORY_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getTapAuthHistory(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapAuthHistory(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `can retrieve history for authorisation`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    manageUsers.findUser(DEFAULT_USERNAME, user(DEFAULT_USERNAME, DEFAULT_NAME))
    val approvingUser = user("ApprovingUser", "Approved By")
    manageUsers.findUser(approvingUser.username, approvingUser)

    transactionTemplate.executeWithoutResult {
      findTemporaryAbsenceAuthorisation(auth.id)?.applyAbsenceCategorisation(
        RecategoriseAuthorisation(
          absenceTypeCode = "PP",
          absenceSubTypeCode = "PP",
          absenceReasonCategoryCode = null,
          absenceReasonCode = "PC",
          reason = "A reason for changing the categorisation",
          reasonPath = ReasonPath(listOf(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")),
        ),
      ) { domain, code ->
        requireNotNull(referenceDataRepository.findByKey(domain of code))
      }
    }
    val approveAction = ApproveAuthorisation(reason = "A reason for approving")
    transactionTemplate.executeWithoutResult {
      ExternalMovementContext.get().copy(username = approvingUser.username, reason = approveAction.reason).set()
      findTemporaryAbsenceAuthorisation(auth.id)?.approve(approveAction) { domain, code ->
        requireNotNull(referenceDataRepository.findByKey(domain of code))
      }
    }
    val cancelAction = CancelAuthorisation(reason = "A reason for cancelling")
    transactionTemplate.executeWithoutResult {
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = cancelAction.reason).set()
      findTemporaryAbsenceAuthorisation(auth.id)?.cancel(cancelAction) { domain, code ->
        requireNotNull(referenceDataRepository.findByKey(domain of code))
      }
    }
    ExternalMovementContext.clear()

    val history = getTapAuthHistory(auth.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(4)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly("person.temporary-absence-authorisation.pending")
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).isEmpty()
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change("absenceType", "Standard ROTL (Release on Temporary Licence)", "Police production"),
        AuditedAction.Change("absenceSubType", "RDR (Resettlement Day Release)", "Police production"),
        AuditedAction.Change("absenceReasonCategory", "Paid work", null),
        AuditedAction.Change("absenceReason", "IT and communication", "Police Production"),
      )
    }
    with(history.content[2]) {
      assertThat(user).isEqualTo(AuditedAction.User(approvingUser.username, approvingUser.name))
      assertThat(domainEvents).containsExactly("person.temporary-absence-authorisation.approved")
      assertThat(reason).isEqualTo(approveAction.reason)
      assertThat(changes).containsExactly(AuditedAction.Change("status", "To be reviewed", "Approved"))
    }
    with(history.content.last()) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).containsExactly("person.temporary-absence-authorisation.cancelled")
      assertThat(reason).isEqualTo(cancelAction.reason)
      assertThat(changes).containsExactly(AuditedAction.Change("status", "Approved", "Cancelled"))
    }
  }

  private fun getTapAuthHistory(
    id: UUID,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_TAP_AUTHORISATION_HISTORY_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_AUTHORISATION_HISTORY_URL = "/temporary-absence-authorisations/{id}/history"
  }
}
