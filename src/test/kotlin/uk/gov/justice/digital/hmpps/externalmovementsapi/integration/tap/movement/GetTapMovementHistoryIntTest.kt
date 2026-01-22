package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.movement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOut
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRelocated
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.movement.ChangeMovementLocation
import java.util.UUID

class GetTapMovementHistoryIntTest(
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
      .uri(GET_TAP_MOVEMENT_HISTORY_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden without correct role`(role: String) {
    getTapMovementHistory(newUuid(), role).expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapMovementHistory(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `can retrieve history for movement`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.OUT,
            auth.person.identifier,
            accompaniedBy = "PECS",
          ),
        ),
      ),
    )
    val movement = occurrence.movements().first()
    manageUsers.findUser(DEFAULT_USERNAME, user(DEFAULT_USERNAME, DEFAULT_NAME))
    val locationUser = user("LocationUser", "Location User")
    manageUsers.findUser(locationUser.username, locationUser)

    val originalLocation = movement.location
    val changeLocation = ChangeMovementLocation(location = location(), reason = "A reason for changing the location")
    ExternalMovementContext.get().copy(username = locationUser.username, reason = changeLocation.reason).set()
    transactionTemplate.executeWithoutResult {
      findTemporaryAbsenceMovement(movement.id)?.applyLocation(changeLocation)
    }

    val accompaniedAction = ChangeMovementAccompaniment(
      "U",
      "This person can go unaccompanied now",
      "A reason the person can be unaccompanied",
    )
    ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = accompaniedAction.reason).set()
    transactionTemplate.executeWithoutResult {
      findTemporaryAbsenceMovement(movement.id)?.applyAccompaniedBy(accompaniedAction) { domain, code ->
        requireNotNull(referenceDataRepository.findAll().first { domain.isInstance(it) && it.code == code })
      }
    }
    ExternalMovementContext.clear()

    val history = getTapMovementHistory(movement.id).successResponse<AuditHistory>()
    assertThat(history.content).hasSize(3)
    with(history.content.first()) {
      assertThat(user).isEqualTo(AuditedAction.User(SYSTEM_USERNAME, "User $SYSTEM_USERNAME"))
      assertThat(domainEvents).containsExactly(TapMovementOut.EVENT_TYPE)
      assertThat(reason).startsWith("Recorded as having gone out of the prison on")
    }
    with(history.content[1]) {
      assertThat(user).isEqualTo(AuditedAction.User(locationUser.username, locationUser.name))
      assertThat(domainEvents).containsExactly(TapMovementRelocated.EVENT_TYPE)
      assertThat(reason).isEqualTo("A reason for changing the location")
      with(changes.first()) {
        assertThat(previous).isEqualTo(originalLocation.toString())
        assertThat(change).isEqualTo(changeLocation.location.toString())
      }
    }
    with(history.content.last()) {
      assertThat(user).isEqualTo(AuditedAction.User(DEFAULT_USERNAME, DEFAULT_NAME))
      assertThat(domainEvents).containsExactly(TapMovementAccompanimentChanged.EVENT_TYPE)
      assertThat(reason).isEqualTo(accompaniedAction.reason)
      assertThat(changes).containsExactlyInAnyOrder(
        AuditedAction.Change(
          propertyName = "accompaniedBy",
          previous = "Prisoner escort contractors (other)",
          change = "Unaccompanied",
        ),
        AuditedAction.Change(
          propertyName = "accompaniedByComments",
          previous = "Some comments about the accompanied by",
          change = "This person can go unaccompanied now",
        ),
      )
    }
  }

  private fun getTapMovementHistory(
    id: UUID,
    role: String? = EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_TAP_MOVEMENT_HISTORY_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_MOVEMENT_HISTORY_URL = "/temporary-absence-movements/{id}/history"
  }
}
