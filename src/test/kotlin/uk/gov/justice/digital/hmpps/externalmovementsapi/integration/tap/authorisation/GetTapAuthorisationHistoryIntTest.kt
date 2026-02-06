package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
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
        locations = linkedSetOf(location()),
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(TempAbsenceOccurrenceOperations.temporaryAbsenceOccurrence(auth))
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
    assertThat(history.content).hasSize(5)
    with(history.content.first()) {
      assertThat(user)
        .isEqualTo(AuditedAction.User(ExternalMovementContext.SYSTEM_USERNAME, "User ${ExternalMovementContext.SYSTEM_USERNAME}"))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationPending.EVENT_TYPE)
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(ExternalMovementContext.SYSTEM_USERNAME, "User ${ExternalMovementContext.SYSTEM_USERNAME}"))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationRelocated.EVENT_TYPE)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change("locations", auth.locations.map { it.toString() }, listOf(occ.location.toString())),
      )
    }
    with(history.content[2]) {
      assertThat(user)
        .isEqualTo(AuditedAction.User(ExternalMovementContext.SYSTEM_USERNAME, "User ${ExternalMovementContext.SYSTEM_USERNAME}"))
      assertThat(domainEvents).contains(TemporaryAbsenceAuthorisationRecategorised.EVENT_TYPE)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change("absenceType", "Standard ROTL (release on temporary licence)", "Police production"),
        AuditedAction.Change("absenceSubType", "RDR (resettlement day release)", "Police production"),
        AuditedAction.Change("absenceReasonCategory", "Paid work", null),
        AuditedAction.Change("absenceReason", "IT and communication", "Police production"),
      )
    }
    with(history.content[3]) {
      assertThat(user).isEqualTo(AuditedAction.User(approvingUser.username, approvingUser.name))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationApproved.EVENT_TYPE)
      assertThat(reason).isEqualTo(approveAction.reason)
      assertThat(changes).containsExactly(AuditedAction.Change("status", "To be reviewed", "Approved"))
    }
    with(history.content.last()) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceAuthorisationCancelled.EVENT_TYPE)
      assertThat(reason).isEqualTo(cancelAction.reason)
      assertThat(changes).containsExactly(AuditedAction.Change("status", "Approved", "Cancelled"))
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
