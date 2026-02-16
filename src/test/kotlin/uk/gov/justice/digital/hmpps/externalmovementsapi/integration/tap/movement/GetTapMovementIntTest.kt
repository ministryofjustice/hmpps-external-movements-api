package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.movement

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.IN
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.verifyAgainst
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapMovement
import java.time.LocalDateTime
import java.util.UUID

class GetTapMovementIntTest(
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

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden without correct role`(role: String) {
    getTapMovement(newUuid(), role).expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapMovement(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `can retrieve out movement`() {
    val prison = givenPrison()
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prison.code))
    val occurrence =
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          end = LocalDateTime.now().plusHours(2),
          movements = listOf(temporaryAbsenceMovement(OUT, auth.person.identifier, prisonCode = prison.code)),
        ),
      )
    val movement = occurrence.movements().first()

    val response = getTapMovement(movement.id).successResponse<TapMovement>()
    response.verifyAgainst(movement)
    assertThat(response.occurrence?.status?.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)
  }

  @Test
  fun `can retrieve in movement`() {
    val prison = givenPrison()
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prison.code))
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusHours(4),
        end = LocalDateTime.now().plusHours(2),
        movements = listOf(
          temporaryAbsenceMovement(
            OUT,
            auth.person.identifier,
            occurredAt = LocalDateTime.now().minusHours(4),
            prisonCode = prison.code,
          ),
          temporaryAbsenceMovement(
            IN,
            auth.person.identifier,
            occurredAt = LocalDateTime.now(),
            prisonCode = prison.code,
          ),
        ),
      ),
    )

    val movement = occurrence.movements().last()

    val response = getTapMovement(movement.id).successResponse<TapMovement>()
    response.verifyAgainst(movement)
    assertThat(response.occurrence?.status?.code).isEqualTo(OccurrenceStatus.Code.COMPLETED.name)
  }

  private fun getTapMovement(
    id: UUID,
    role: String? = EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_TAP_MOVEMENT_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_MOVEMENT_URL = "/temporary-absence-movements/{id}"
  }
}
