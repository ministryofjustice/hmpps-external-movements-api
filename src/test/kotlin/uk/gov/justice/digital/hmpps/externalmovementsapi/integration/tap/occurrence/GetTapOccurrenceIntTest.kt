package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.occurrence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement.Direction.IN
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapOccurrence
import java.time.LocalDateTime
import java.util.UUID

class GetTapOccurrenceIntTest(
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
  fun `can retrieve individually cancelled occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        cancelledAt = LocalDateTime.now(),
        cancelledBy = "C4nc3113d",
      ),
    )

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("CANCELLED")
  }

  @Test
  fun `can retrieve scheduled occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, end = LocalDateTime.now().plusHours(2)))

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("SCHEDULED")
  }

  @Test
  fun `can retrieve expired occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, end = LocalDateTime.now().minusHours(2)))

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("EXPIRED")
  }

  @Test
  fun `can retrieve pending occurrence`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = TapAuthorisationStatus.Code.PENDING))
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("PENDING")
  }

  @Test
  fun `can retrieve cancelled occurrence from cancelled authorisation`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = TapAuthorisationStatus.Code.CANCELLED))
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("CANCELLED")
  }

  @Test
  fun `can retrieve denied occurrence`() {
    val auth =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = TapAuthorisationStatus.Code.DENIED))
    val occurrence =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("DENIED")
  }

  @Test
  fun `can retrieve overdue occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          end = LocalDateTime.now().minusHours(2),
          movements = listOf(temporaryAbsenceMovement(OUT, auth.person.identifier)),
        ),
      )

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("OVERDUE")
  }

  @Test
  fun `can retrieve in progress occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          end = LocalDateTime.now().plusHours(2),
          movements = listOf(temporaryAbsenceMovement(OUT, auth.person.identifier)),
        ),
      )

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("IN_PROGRESS")
  }

  @Test
  fun `can retrieve completed occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
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
          ),
          temporaryAbsenceMovement(
            IN,
            auth.person.identifier,
            occurredAt = LocalDateTime.now(),
          ),
        ),
      ),
    )

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    occurrence.verifyAgainst(response)
    assertThat(response.location).isEqualTo(occurrence.location)
    assertThat(response.status.code).isEqualTo("COMPLETED")
  }

  @Test
  fun `can retrieve occurrence with position and total count`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ1 =
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          auth,
          start = LocalDateTime.now().plusHours(1),
          end = LocalDateTime.now().plusHours(3),
        ),
      )
    val occ2 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusHours(6),
        end = LocalDateTime.now().plusHours(8),
      ),
    )
    val occ3 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusHours(10),
        end = LocalDateTime.now().plusHours(12),
      ),
    )

    val res1 = getTapOccurrence(occ1.id).successResponse<TapOccurrence>()
    occ1.verifyAgainst(res1)
    assertThat(res1.occurrencePosition).isEqualTo(1)
    assertThat(res1.totalOccurrences).isEqualTo(3)

    val res2 = getTapOccurrence(occ2.id).successResponse<TapOccurrence>()
    occ2.verifyAgainst(res2)
    assertThat(res2.occurrencePosition).isEqualTo(2)
    assertThat(res2.totalOccurrences).isEqualTo(3)

    val res3 = getTapOccurrence(occ3.id).successResponse<TapOccurrence>()
    occ3.verifyAgainst(res3)
    assertThat(res3.occurrencePosition).isEqualTo(3)
    assertThat(res3.totalOccurrences).isEqualTo(3)
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
