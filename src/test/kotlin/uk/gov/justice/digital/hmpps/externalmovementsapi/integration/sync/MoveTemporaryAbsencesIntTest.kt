package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.IN
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction.OUT
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.MoveTemporaryAbsencesRequest
import java.util.SequencedSet
import java.util.UUID

class MoveTemporaryAbsencesIntTest(
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
      .put()
      .uri(MOVE_TAP, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    moveTap(
      moveTapRequest(),
      listOf(
        Roles.EXTERNAL_MOVEMENTS_RO,
        Roles.EXTERNAL_MOVEMENTS_UI,
        Roles.TEMPORARY_ABSENCE_RO,
        Roles.TEMPORARY_ABSENCE_RW,
      ),
    ).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if id not linked to from person identifier`() {
    val toPersonIdentifier = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode(), setOf(toPersonIdentifier))
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    moveTap(moveTapRequest(toPersonIdentifier = toPersonIdentifier, authorisationIds = sortedSetOf(auth.id)))
      .expectStatus().isBadRequest

    val unchanged = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(unchanged.person.identifier).isEqualTo(auth.person.identifier)
  }

  @Test
  fun `200 ok - can move selected absence details to another person identifier`() {
    val prisonCode = prisonCode()
    val p1 = personIdentifier()
    givenPersonSummary(personSummary(p1))
    val p2 = personIdentifier()
    givenPersonSummary(personSummary(p2))

    val p1Auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, p1))
    val p1Occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(p1Auth1))
    val p1Auth2 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, p1))
    val p1Occ2 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(p1Auth2))
    val p1Occ3 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(p1Auth2))
    val p1Sm1 = givenTemporaryAbsenceMovement(temporaryAbsenceMovement(OUT, p1, p1Occ2))
    val p1Um1 = givenTemporaryAbsenceMovement(temporaryAbsenceMovement(IN, p1))

    val p2Auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, p2))
    val p2Occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(p2Auth1))

    moveTap(
      moveTapRequest(
        p1,
        p2,
        sortedSetOf(p1Auth2.id),
        sortedSetOf(p1Um1.id),
      ),
    ).expectStatus().isOk

    with(requireNotNull(findTemporaryAbsenceOccurrence(p1Occ1.id))) {
      assertThat(authorisation.person.identifier).isEqualTo(p1)
    }

    val context = ExternalMovementContext.get().copy(reason = "Prisoner booking moved", source = DataSource.NOMIS)
    with(requireNotNull(findTemporaryAbsenceOccurrence(p1Occ2.id))) {
      assertThat(authorisation.person.identifier).isEqualTo(p2)
      verifyAudit(
        this.authorisation,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }
    with(requireNotNull(findTemporaryAbsenceMovement(p1Sm1.id))) {
      assertThat(person.identifier).isEqualTo(p2)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }
    with(requireNotNull(findTemporaryAbsenceOccurrence(p1Occ3.id))) {
      assertThat(authorisation.person.identifier).isEqualTo(p2)
      verifyAudit(
        this.authorisation,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }
    with(requireNotNull(findTemporaryAbsenceMovement(p1Um1.id))) {
      assertThat(person.identifier).isEqualTo(p2)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }

    with(requireNotNull(findTemporaryAbsenceOccurrence(p2Occ1.id))) {
      assertThat(authorisation.person.identifier).isEqualTo(p2)
    }
  }

  private fun moveTapRequest(
    fromPersonIdentifier: String = personIdentifier(),
    toPersonIdentifier: String = personIdentifier(),
    authorisationIds: SequencedSet<UUID> = sortedSetOf(),
    unscheduledMovementIds: SequencedSet<UUID> = sortedSetOf(),
  ) = MoveTemporaryAbsencesRequest(
    fromPersonIdentifier,
    toPersonIdentifier,
    authorisationIds,
    unscheduledMovementIds,
  )

  private fun moveTap(
    request: MoveTemporaryAbsencesRequest,
    roles: List<String> = listOf(Roles.NOMIS_SYNC),
  ) = webTestClient
    .put()
    .uri(MOVE_TAP)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = roles))
    .exchange()

  companion object {
    const val MOVE_TAP = "/move/temporary-absences"
  }
}
