package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.occurrence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.IN
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCompleted
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceStarted
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersServer.Companion.user
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
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

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden without correct role`(role: String) {
    getTapOccurrenceHistory(newUuid(), role).expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapOccurrenceHistory(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `can retrieve history for occurrence`() {
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
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
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
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
    }
    ExternalMovementContext.clear()

    val history = getTapOccurrenceHistory(occurrence.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(4)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceScheduled.EVENT_TYPE)
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).contains(TemporaryAbsenceRecategorised.EVENT_TYPE)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change("absenceType", "Standard ROTL (release on temporary licence)", "Police production"),
        AuditedAction.Change("absenceSubType", "RDR (resettlement day release)", "Police production"),
        AuditedAction.Change("absenceReasonCategory", "Paid work", null),
        AuditedAction.Change("absenceReason", "IT and communication", "Police production"),
      )
    }
    with(history.content[2]) {
      assertThat(user).isEqualTo(AuditedAction.User(locationUser.username, locationUser.name))
      assertThat(domainEvents).contains(TemporaryAbsenceRelocated.EVENT_TYPE)
      assertThat(reason).isEqualTo("A reason for changing the location")
      with(changes.first()) {
        assertThat(previous).isEqualTo(originalLocation.toString())
        assertThat(change).isEqualTo(changeLocation.location.toString())
      }
    }
    with(history.content.last()) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceCancelled.EVENT_TYPE)
      assertThat(reason).isEqualTo("A reason for cancelling")
    }
  }

  @Test
  fun `occurrence shows started and completed`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    manageUsers.findUser(DEFAULT_USERNAME, user(DEFAULT_USERNAME, DEFAULT_NAME))

    transactionTemplate.executeWithoutResult {
      val movement = givenTemporaryAbsenceMovement(
        temporaryAbsenceMovement(OUT, auth.person.identifier, occurrence),
      )
      findTemporaryAbsenceOccurrence(occurrence.id)?.addMovement(movement) { code ->
        requireNotNull(
          referenceDataRepository.findAll().first { it is OccurrenceStatus && it.code == code } as OccurrenceStatus,
        )
      }
    }
    transactionTemplate.executeWithoutResult {
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME).set()
      val movement = givenTemporaryAbsenceMovement(
        temporaryAbsenceMovement(IN, auth.person.identifier, occurrence),
      )
      findTemporaryAbsenceOccurrence(occurrence.id)?.addMovement(movement) { code ->
        requireNotNull(
          referenceDataRepository.findAll().first { it is OccurrenceStatus && it.code == code } as OccurrenceStatus,
        )
      }
    }
    ExternalMovementContext.clear()

    val history = getTapOccurrenceHistory(occurrence.id).successResponse<AuditHistory>()
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly(TemporaryAbsenceScheduled.EVENT_TYPE)
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).contains(TemporaryAbsenceStarted.EVENT_TYPE)
      assertThat(changes).containsExactly(
        AuditedAction.Change(propertyName = "status", previous = "Scheduled", change = "In progress"),
      )
    }
    with(history.content.last()) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).contains(TemporaryAbsenceCompleted.EVENT_TYPE)
      assertThat(changes).containsExactly(
        AuditedAction.Change(propertyName = "status", previous = "In progress", change = "Completed"),
      )
    }
  }

  @Test
  fun `when multiple changes applied a single instance of an event is generated per transaction`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))

    transactionTemplate.executeWithoutResult {
      val occ = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
      occ.applyComments(ChangeOccurrenceComments("The comments are changed once"))
      // flush forces attempt to regenerate domain events
      entityManager.flush()
      occ.applyLocation(ChangeOccurrenceLocation(location()))
    }
    ExternalMovementContext.clear()

    val history = getTapOccurrenceHistory(occurrence.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(2)
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly(
        TemporaryAbsenceCommentsChanged.EVENT_TYPE,
        TemporaryAbsenceRelocated.EVENT_TYPE,
        TemporaryAbsenceAuthorisationRelocated.EVENT_TYPE,
      )
    }
  }

  private fun getTapOccurrenceHistory(
    id: UUID,
    role: String? = listOf(Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW).random(),
  ) = webTestClient
    .get()
    .uri(GET_TAP_OCCURRENCE_HISTORY_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_OCCURRENCE_HISTORY_URL = "/temporary-absence-occurrences/{id}/history"
  }
}
