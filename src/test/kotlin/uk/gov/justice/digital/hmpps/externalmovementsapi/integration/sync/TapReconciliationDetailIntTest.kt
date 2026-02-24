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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonTapDetail

class TapReconciliationDetailIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
  @Autowired private val occStatusRepository: OccurrenceStatusRepository,
  @Autowired private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(RECONCILE_TAP_DETAIL_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    getTapDetail(personIdentifier(), role).expectStatus().isForbidden
  }

  @Test
  fun `200 ok when no data for person identifier`() {
    getTapDetail(personIdentifier()).expectStatus().isOk
  }

  @Test
  fun `200 ok - tap detail returned for person`() {
    val personIdentifier = personIdentifier()
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(personIdentifier = personIdentifier, repeat = true))
    val auth2 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        personIdentifier = personIdentifier,
        repeat = true,
        status = AuthorisationStatus.Code.PENDING,
      ),
    )
    val occs = (1..5).map {
      val auth = if (it % 2 == 0) auth2 else auth1
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    }
    val movements = (1..6).map {
      val occurrence = if (it % 2 == 0) null else occs.random()
      val direction = if (occurrence == null) {
        if (it < 4) Direction.OUT else Direction.IN
      } else {
        if (it > 3) Direction.IN else Direction.OUT
      }
      givenTemporaryAbsenceMovement(
        temporaryAbsenceMovement(direction = direction, personIdentifier = personIdentifier, occurrence = occurrence),
      )
    }

    val occurrences = occs.map {
      occurrenceRepository.save(
        findTemporaryAbsenceOccurrence(it.id)!!.calculateStatus { code ->
          occStatusRepository.getByCode(code)
        },
      )
    }

    val auth3 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        personIdentifier = personIdentifier,
        repeat = false,
        status = AuthorisationStatus.Code.PENDING,
      ),
    )
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth3, dpsOnly = true))

    val res = getTapDetail(personIdentifier).successResponse<PersonTapDetail>()
    assertThat(res.scheduledAbsences.map { it.id to it.statusCode }).containsExactlyInAnyOrder(
      auth1.id to AuthorisationStatus.Code.valueOf(auth1.status.code),
      auth2.id to AuthorisationStatus.Code.valueOf(auth2.status.code),
      auth3.id to AuthorisationStatus.Code.valueOf(auth3.status.code),
    )
    val schedules = res.scheduledAbsences.flatMap { it.occurrences }
    assertThat(schedules.map { it.id to it.statusCode }).containsExactlyInAnyOrderElementsOf(
      occurrences.map { it.id to OccurrenceStatus.Code.valueOf(it.status.code) },
    )
    val scheduled = schedules.flatMap { it.movements }
    assertThat(scheduled.map { it.id to it.direction }).containsExactlyInAnyOrderElementsOf(
      movements.filter { it.occurrence != null }.map { it.id to it.direction },
    )
    assertThat(res.unscheduledMovements.map { it.id to it.direction }).containsExactlyInAnyOrderElementsOf(
      movements.filter { it.occurrence == null }.map { it.id to it.direction },
    )
  }

  private fun getTapDetail(
    personIdentifier: String,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(RECONCILE_TAP_DETAIL_URL, personIdentifier)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val RECONCILE_TAP_DETAIL_URL = "/reconciliation-detail/{personIdentifier}/temporary-absences"
  }
}
