package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapOccurrence
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class RetrieveTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
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

  @Test
  fun `200 ok returns SE subtype when type is SE`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        absenceType = "SE",
        absenceSubType = null,
        absenceReasonCategory = null,
        absenceReason = "C6",
      ),
    )
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
  assertThat(absenceTypeCode).isEqualTo(occurrence.absenceType?.code)
  if (absenceTypeCode == "SE") {
    assertThat(absenceSubTypeCode).isEqualTo("SE")
  } else {
    assertThat(absenceSubTypeCode).isEqualTo(occurrence.absenceSubType?.code)
  }
  assertThat(absenceReasonCode).isEqualTo(occurrence.absenceReason?.code)
  assertThat(statusCode).isEqualTo(occurrence.status.code)
  assertThat(start).isCloseTo(occurrence.start, within(2, SECONDS))
  assertThat(end).isCloseTo(occurrence.end, within(2, SECONDS))
  assertThat(location).isEqualTo(occurrence.location)
  assertThat(accompaniedByCode).isEqualTo(occurrence.accompaniedBy.code)
  assertThat(transportCode).isEqualTo(occurrence.transport.code)
  assertThat(comments).isEqualTo(occurrence.comments)
}

private fun TapOccurrence.Authorisation.verifyAgainst(authorisation: TemporaryAbsenceAuthorisation) {
  assertThat(id).isEqualTo(authorisation.id)
  assertThat(personIdentifier).isEqualTo(authorisation.person.identifier)
  assertThat(prisonCode).isEqualTo(authorisation.prisonCode)
}
