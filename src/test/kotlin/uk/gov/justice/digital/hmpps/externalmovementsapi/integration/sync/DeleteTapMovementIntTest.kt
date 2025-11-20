package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import java.util.UUID

class DeleteTapMovementIntTest(
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
      .delete()
      .uri(DELETE_TAP_MOVEMENT_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    deleteTapMovement(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `204 no content when id invalid or already deleted`() {
    deleteTapMovement(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 can delete tap movement`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = requireNotNull(
      findTemporaryAbsenceOccurrence(givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)).id),
    )
    val movement = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        direction = TemporaryAbsenceMovement.Direction.OUT,
        personIdentifier = occurrence.authorisation.personIdentifier,
        occurrence = occurrence,
      ),
    )

    deleteTapMovement(movement.id).expectStatus().isNoContent
    val saved = findTemporaryAbsenceMovement(movement.id)
    assertThat(saved).isNull()

    verifyAudit(
      movement,
      RevisionType.DEL,
      setOf(TemporaryAbsenceMovement::class.simpleName!!, TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyAudit(
      occurrence,
      RevisionType.MOD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!, TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(movement, setOf())
  }

  private fun deleteTapMovement(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .delete()
    .uri(DELETE_TAP_MOVEMENT_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val DELETE_TAP_MOVEMENT_URL = "/sync/temporary-absence-movements/{id}"
  }
}
