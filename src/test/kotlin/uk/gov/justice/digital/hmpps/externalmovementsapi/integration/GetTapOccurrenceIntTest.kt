package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement.Direction.IN
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersExtension.Companion.manageUsers
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersServer.Companion.user
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import java.time.LocalDateTime
import java.util.UUID

class GetTapOccurrenceIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_TAP_OCCURRENCE_URL, newUuid())
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
  fun `can retrieve cancelled occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        cancelledAt = LocalDateTime.now(),
        cancelledBy = "C4nc3113d",
      ),
    )
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val addingUser = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, addingUser)
    val cancellingUser = user(occurrence.cancelledBy!!, "The cancelling user")
    manageUsers.findUser(occurrence.cancelledBy!!, cancellingUser)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(addingUser.name)
    assertThat(response.cancelled!!.displayName).isEqualTo(cancellingUser.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("CANCELLED")
  }

  @Test
  fun `can retrieve scheduled occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, returnBy = LocalDateTime.now().plusHours(2)))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("SCHEDULED")
  }

  @Test
  fun `can retrieve expired occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, returnBy = LocalDateTime.now().minusHours(2)))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("EXPIRED")
  }

  @Test
  fun `can retrieve pending occurrence`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = TapAuthorisationStatus.Code.PENDING))
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("PENDING")
  }

  @Test
  fun `can retrieve withdrawn occurrence`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = TapAuthorisationStatus.Code.WITHDRAWN))
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("WITHDRAWN")
  }

  @Test
  fun `can retrieve denied occurrence`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = TapAuthorisationStatus.Code.DENIED))
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("DENIED")
  }

  @Test
  fun `can retrieve overdue occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, returnBy = LocalDateTime.now().minusHours(2)))
    givenTemporaryAbsenceMovement(temporaryAbsenceMovement(OUT, occurrence = occurrence))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("OVERDUE")
  }

  @Test
  fun `can retrieve in progress occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, returnBy = LocalDateTime.now().plusHours(2)))
    givenTemporaryAbsenceMovement(temporaryAbsenceMovement(OUT, occurrence = occurrence))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("IN_PROGRESS")
  }

  @Test
  fun `can retrieve completed occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        releaseAt = LocalDateTime.now().minusHours(4),
        returnBy = LocalDateTime.now().plusHours(2),
      ),
    )
    givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        OUT,
        occurrence = occurrence,
        occurredAt = LocalDateTime.now().minusHours(4),
      ),
    )
    givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        IN,
        occurrence = occurrence,
        occurredAt = LocalDateTime.now(),
      ),
    )
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(occurrence.personIdentifier))
    val user = user(occurrence.addedBy, "The adding user")
    manageUsers.findUser(occurrence.addedBy, user)

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.added.displayName).isEqualTo(user.name)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("COMPLETED")
  }

  private fun getTapOccurrence(
    id: UUID,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_TAP_OCCURRENCE_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_OCCURRENCE_URL = "/temporary-absence-occurrences/{id}"
  }
}
