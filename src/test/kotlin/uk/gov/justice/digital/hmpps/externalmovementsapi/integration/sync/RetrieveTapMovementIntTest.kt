package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapMovement
import java.util.UUID

class RetrieveTapMovementIntTest(
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
      .uri(GET_TAP_OCCUR_URL, newUuid())
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
        personIdentifier = occurrence.personIdentifier,
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
    .uri(GET_TAP_OCCUR_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_OCCUR_URL = "/sync/temporary-absence-movements/{id}"
  }
}

private fun TapMovement.verifyAgainst(movement: TemporaryAbsenceMovement) {
  assertThat(id).isEqualTo(movement.id)
  assertThat(occurrenceId).isEqualTo(movement.occurrence?.id)
  assertThat(direction).isEqualTo(movement.direction)
  assertThat(absenceReasonCode).isEqualTo(movement.absenceReason.code)
  assertThat(location).isEqualTo(movement.location)
  assertThat(accompaniedByCode).isEqualTo(movement.accompaniedBy.code)
  assertThat(accompaniedByNotes).isEqualTo(movement.accompaniedByNotes)
  assertThat(notes).isEqualTo(movement.notes)
  assertThat(recordedByPrisonCode).isEqualTo(movement.recordedByPrisonCode)
}
