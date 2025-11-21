package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import java.time.LocalDateTime
import java.util.UUID

class GetTapAuthorisationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_TAP_AUTH_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getTapAuthorisation(newUuid(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found when id invalid`() {
    getTapAuthorisation(newUuid()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok finds tap authorisation and occurrences`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val firstOcc = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        releaseAt = LocalDateTime.now().plusDays(3),
        returnBy = LocalDateTime.now().plusDays(4),
        location = firstOcc.location,
      ),
    )
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(auth.personIdentifier))

    val response = getTapAuthorisation(auth.id).successResponse<TapAuthorisation>()
    response.verifyAgainst(auth)
    assertThat(response.locations).hasSize(1)
    firstOcc.verifyAgainst(response.occurrences.first())
  }

  @Test
  fun `200 ok finds tap authorisation created with just type`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        absenceType = "PP",
        absenceSubType = "PP",
        absenceReasonCategory = null,
        absenceReason = "PC",
        reasonPath = ReasonPath(listOf(ABSENCE_TYPE of "PP")),
      ),
    )
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    prisonerSearch.getPrisoners(auth.prisonCode, setOf(auth.personIdentifier))

    val response = getTapAuthorisation(auth.id).successResponse<TapAuthorisation>()
    assertThat(response.absenceType?.code).isEqualTo("PP")
    assertThat(response.absenceSubType).isNull()
    assertThat(response.absenceReasonCategory).isNull()
    assertThat(response.absenceReason).isNull()
    response.verifyAgainst(auth)
    occurrence.verifyAgainst(response.occurrences.first())
  }

  private fun getTapAuthorisation(
    id: UUID,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_TAP_AUTH_URL, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_AUTH_URL = "/temporary-absence-authorisations/{id}"
  }
}
