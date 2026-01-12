package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonTapCounts

class TapReconciliationCountIntTest(
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
      .uri(RECONCILE_TAP_COUNT_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    getTapCounts(personIdentifier(), role).expectStatus().isForbidden
  }

  @Test
  fun `200 ok when no data for person identifier`() {
    getTapCounts(personIdentifier()).expectStatus().isOk
  }

  @Test
  fun `200 ok - counts returned for person`() {
    val personIdentifier = personIdentifier()
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(personIdentifier = personIdentifier))
    val auth2 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(personIdentifier = personIdentifier))
    val occurrences = (1..5).map {
      val auth = if (it % 2 == 0) auth2 else auth1
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    }
    (1..6).map {
      val occurrence = if (it % 2 == 0) null else occurrences.random()
      val direction = if (occurrence == null) {
        if (it < 4) Direction.OUT else Direction.IN
      } else {
        if (it > 3) Direction.IN else Direction.OUT
      }
      givenTemporaryAbsenceMovement(
        temporaryAbsenceMovement(direction = direction, personIdentifier = personIdentifier, occurrence = occurrence),
      )
    }
    val res = getTapCounts(personIdentifier).successResponse<PersonTapCounts>()

    assertThat(res.authorisations.count).isEqualTo(2)
    assertThat(res.occurrences.count).isEqualTo(5)
    assertThat(res.movements.scheduled.outCount).isEqualTo(2)
    assertThat(res.movements.scheduled.inCount).isEqualTo(1)
    assertThat(res.movements.unscheduled.outCount).isEqualTo(1)
    assertThat(res.movements.unscheduled.inCount).isEqualTo(2)
  }

  private fun getTapCounts(
    personIdentifier: String,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(RECONCILE_TAP_COUNT_URL, personIdentifier)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RECONCILE_TAP_COUNT_URL = "/reconciliation/{personIdentifier}/temporary-absences"
  }
}
