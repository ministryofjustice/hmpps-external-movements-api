package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import com.fasterxml.jackson.module.kotlin.treeToValue
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.AuthorisationSchedule
import java.util.UUID

class DeleteTapOccurrenceIntTest(
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
      .uri(DELETE_TAP_OCCUR_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    deleteTapOccurrence(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `409 cannot delete occurrence with movement`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      requireNotNull(findTemporaryAbsenceOccurrence(givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)).id))
    givenTemporaryAbsenceMovement(temporaryAbsenceMovement(TemporaryAbsenceMovement.Direction.OUT, occurrence.authorisation.person.identifier, occurrence))

    val res = deleteTapOccurrence(occurrence.id).errorResponse(HttpStatus.CONFLICT)

    val saved = findTemporaryAbsenceOccurrence(occurrence.id)
    assertThat(saved).isNotNull

    assertThat(res.userMessage).isEqualTo("Cannot delete an occurrence with a movement")
  }

  @Test
  fun `204 no content when id invalid or already deleted`() {
    deleteTapOccurrence(newUuid()).expectStatus().isNoContent
  }

  @Test
  fun `204 can delete occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      requireNotNull(findTemporaryAbsenceOccurrence(givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)).id))

    deleteTapOccurrence(occurrence.id).expectStatus().isNoContent

    val saved = findTemporaryAbsenceOccurrence(occurrence.id)
    assertThat(saved).isNull()
    val updatedAuth = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(updatedAuth.schedule).isNotNull
    val schedule = objectMapper.treeToValue<AuthorisationSchedule>(updatedAuth.schedule!!)
    assertThat(schedule.startTime).isEqualTo(occurrence.start.toLocalTime())
    assertThat(schedule.returnTime).isEqualTo(occurrence.end.toLocalTime())

    verifyAudit(
      occurrence,
      RevisionType.DEL,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, TemporaryAbsenceAuthorisation::class.simpleName!!),
      ExternalMovementContext.get().copy(username = SYSTEM_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(occurrence, setOf())
  }

  private fun deleteTapOccurrence(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .delete()
    .uri(DELETE_TAP_OCCUR_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val DELETE_TAP_OCCUR_URL = "/sync/temporary-absence-occurrences/{id}"
  }
}
