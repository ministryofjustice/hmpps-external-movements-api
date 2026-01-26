package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersExtension
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersServer
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
      .uri(GET_TAP_AUTHORISATION_HISTORY_URL, IdGenerator.newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden without correct role`(role: String) {
    getTapAuthHistory(IdGenerator.newUuid(), role).expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapAuthHistory(IdGenerator.newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `can retrieve history for authorisation`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        status = AuthorisationStatus.Code.PENDING,
      ),
    )
    givenTemporaryAbsenceOccurrence(TempAbsenceOccurrenceOperations.temporaryAbsenceOccurrence(auth))
    ManageUsersExtension.manageUsers.findUser(
      DEFAULT_USERNAME,
      ManageUsersServer.user(DEFAULT_USERNAME, DEFAULT_NAME),
    )
    val approvingUser = ManageUsersServer.user("ApprovingUser", "Approved By")
    ManageUsersExtension.manageUsers.findUser(approvingUser.username, approvingUser)

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
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
    }
    val approveAction = ApproveAuthorisation(reason = "A reason for approving")
    transactionTemplate.executeWithoutResult {
      ExternalMovementContext.get().copy(username = approvingUser.username, reason = approveAction.reason).set()
      findTemporaryAbsenceAuthorisation(auth.id)?.approve(approveAction) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
    }
    val cancelAction = CancelAuthorisation(reason = "A reason for cancelling")
    transactionTemplate.executeWithoutResult {
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = cancelAction.reason).set()
      findTemporaryAbsenceAuthorisation(auth.id)?.cancel(cancelAction) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
    }
    ExternalMovementContext.clear()

    val history = getTapAuthHistory(auth.id).successResponse<AuditHistory>()
    Assertions.assertThat(history.content).hasSize(4)
    with(history.content.first()) {
      Assertions.assertThat(user)
        .isEqualTo(AuditedAction.User(ExternalMovementContext.SYSTEM_USERNAME, "User ${ExternalMovementContext.SYSTEM_USERNAME}"))
      Assertions.assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationPending.EVENT_TYPE)
    }
    with(history.content[1]) {
      Assertions.assertThat(user)
        .isEqualTo(AuditedAction.User(ExternalMovementContext.SYSTEM_USERNAME, "User ${ExternalMovementContext.SYSTEM_USERNAME}"))
      Assertions.assertThat(domainEvents).contains(TemporaryAbsenceAuthorisationRecategorised.EVENT_TYPE)
      Assertions.assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change("absenceType", "Standard ROTL (release on temporary licence)", "Police production"),
        AuditedAction.Change("absenceSubType", "RDR (resettlement day release)", "Police production"),
        AuditedAction.Change("absenceReasonCategory", "Paid work", null),
        AuditedAction.Change("absenceReason", "IT and communication", "Police production"),
      )
    }
    with(history.content[2]) {
      Assertions.assertThat(user).isEqualTo(AuditedAction.User(approvingUser.username, approvingUser.name))
      Assertions.assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationApproved.EVENT_TYPE)
      Assertions.assertThat(reason).isEqualTo(approveAction.reason)
      Assertions.assertThat(changes).containsExactly(AuditedAction.Change("status", "To be reviewed", "Approved"))
    }
    with(history.content.last()) {
      Assertions.assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      Assertions.assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationCancelled.EVENT_TYPE)
      Assertions.assertThat(reason).isEqualTo(cancelAction.reason)
      Assertions.assertThat(changes).containsExactly(AuditedAction.Change("status", "Approved", "Cancelled"))
    }
  }

  private fun getTapAuthHistory(
    id: UUID,
    role: String? = listOf(Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW).random(),
  ) = webTestClient
    .get()
    .uri(GET_TAP_AUTHORISATION_HISTORY_URL, id)
    .headers(setAuthorisation(username = ExternalMovementContext.SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_AUTHORISATION_HISTORY_URL = "/temporary-absence-authorisations/{id}/history"
  }
}
