package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapOccurrence
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class RetrieveTapOccurrenceIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

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
    getTapOccurrence(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapOccurrence(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok finds tap occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence =
      requireNotNull(findTemporaryAbsenceOccurrence(givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth)).id))

    val response = getTapOccurrence(occurrence.id).successResponse<TapOccurrence>()
    response.verifyAgainst(occurrence)
  }

  private fun getTapOccurrence(
    id: UUID,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .get()
    .uri(GET_TAP_OCCUR_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_OCCUR_URL = "/sync/temporary-absence-occurrences/{id}"
  }
}

private fun TapOccurrence.verifyAgainst(occurrence: TemporaryAbsenceOccurrence) {
  assertThat(id).isEqualTo(occurrence.id)
  authorisation.verifyAgainst(occurrence.authorisation)
  assertThat(statusCode).isEqualTo(occurrence.status?.code)
  assertThat(releaseAt).isCloseTo(occurrence.releaseAt, within(2, SECONDS))
  assertThat(returnBy).isCloseTo(occurrence.returnBy, within(2, SECONDS))
  assertThat(location).isEqualTo(occurrence.location)
  assertThat(accompaniedByCode).isEqualTo(occurrence.accompaniedBy.code)
  assertThat(transportCode).isEqualTo(occurrence.transport.code)
  assertThat(notes).isEqualTo(occurrence.notes)
}

private fun TapOccurrence.Authorisation.verifyAgainst(authorisation: TemporaryAbsenceAuthorisation) {
  assertThat(statusCode).isEqualTo(authorisation.status.code)
  assertThat(absenceTypeCode).isEqualTo(authorisation.absenceType?.code)
  assertThat(absenceSubTypeCode).isEqualTo(authorisation.absenceSubType?.code)
  assertThat(absenceReasonCode).isEqualTo(authorisation.absenceReason?.code)
  assertThat(repeat).isEqualTo(authorisation.repeat)
  assertThat(submittedAt).isCloseTo(authorisation.submittedAt, within(2, SECONDS))
}
