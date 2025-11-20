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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersServer.Companion.user
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.util.UUID

class GetTapOccurrenceHistoryIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_TAP_OCCURRENCE_HISTORY_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getTapOccurrence(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapOccurrence(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `can retrieve individually cancelled occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    manageUsers.findUser(DEFAULT_USERNAME, user(DEFAULT_USERNAME, DEFAULT_NAME))
    val locationUser = user("LocationUser", "Location User")
    manageUsers.findUser(locationUser.username, locationUser)

    transactionTemplate.executeWithoutResult {
      findTemporaryAbsenceOccurrence(occurrence.id)?.applyAbsenceCategorisation(
        RecategoriseOccurrence(
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
    val originalLocation = occurrence.location
    val changeLocation = ChangeOccurrenceLocation(location = location(), reason = "A reason for changing the location")
    transactionTemplate.executeWithoutResult {
      ExternalMovementContext.get().copy(username = locationUser.username, reason = changeLocation.reason).set()
      findTemporaryAbsenceOccurrence(occurrence.id)?.applyLocation(changeLocation)
    }
    transactionTemplate.executeWithoutResult {
      val cancelAction = CancelOccurrence(reason = "A reason for cancelling")
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = cancelAction.reason).set()
      findTemporaryAbsenceOccurrence(occurrence.id)?.cancel(cancelAction) { domain, code ->
        requireNotNull(referenceDataRepository.findByKey(domain of code))
      }
    }
    ExternalMovementContext.clear()

    val history = getTapOccurrence(occurrence.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(4)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly("person.temporary-absence.scheduled")
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
      assertThat(user).isEqualTo(AuditedAction.User(locationUser.username, locationUser.name))
      assertThat(domainEvents).isEmpty()
      assertThat(reason).isEqualTo("A reason for changing the location")
      with(changes.first()) {
        assertThat(previous).isEqualTo(originalLocation.asMap())
        assertThat(change).isEqualTo(changeLocation.location.asMap())
      }
    }
    with(history.content.last()) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).containsExactly("person.temporary-absence.cancelled")
      assertThat(reason).isEqualTo("A reason for cancelling")
    }
  }

  private fun Location.asMap() = listOfNotNull(
    description?.let { "description" to it },
    address?.let { "address" to it },
    postcode?.let { "postcode" to it },
    uprn?.let { "uprn" to it },
  ).toMap()

  private fun getTapOccurrence(
    id: UUID,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_TAP_OCCURRENCE_HISTORY_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_OCCURRENCE_HISTORY_URL = "/temporary-absence-occurrences/{id}/history"
  }
}
