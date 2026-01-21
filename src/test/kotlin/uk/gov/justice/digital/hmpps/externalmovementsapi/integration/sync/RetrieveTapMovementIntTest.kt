package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapMovement
import java.util.UUID

class RetrieveTapMovementIntTest(
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
      .uri(GET_TAP_MOVEMENT_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getTapMovement(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapMovement(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok finds tap movement`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      requireNotNull(findTemporaryAbsenceOccurrence(givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)).id))
    val movement = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        direction = TemporaryAbsenceMovement.Direction.OUT,
        personIdentifier = occurrence.authorisation.person.identifier,
        occurrence = occurrence,
      ),
    )

    val response = getTapMovement(movement.id).successResponse<TapMovement>()
    response.verifyAgainst(movement)
  }

  private fun getTapMovement(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(GET_TAP_MOVEMENT_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_MOVEMENT_URL = "/sync/temporary-absence-movements/{id}"
  }
}

private fun TapMovement.verifyAgainst(movement: TemporaryAbsenceMovement) {
  assertThat(id).isEqualTo(movement.id)
  assertThat(occurrenceId).isEqualTo(movement.occurrence?.id)
  assertThat(personIdentifier).isEqualTo(movement.person.identifier)
  assertThat(direction).isEqualTo(movement.direction)
  assertThat(absenceReasonCode).isEqualTo(movement.absenceReason.code)
  assertThat(location).isEqualTo(movement.location)
  assertThat(accompaniedByCode).isEqualTo(movement.accompaniedBy.code)
  assertThat(accompaniedByComments).isEqualTo(movement.accompaniedByComments)
  assertThat(comments).isEqualTo(movement.comments)
  assertThat(created.prisonCode).isEqualTo(movement.recordedByPrisonCode)
}
