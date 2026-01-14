package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync.RetrieveTapAuthorisationIntTest.Companion.GET_TAP_AUTH_URL
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import java.time.LocalDate
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

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden without correct role`(role: String) {
    getTapAuthorisation(newUuid(), role = role).expectStatus().isForbidden
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
        start = LocalDateTime.now().plusDays(3),
        end = LocalDateTime.now().plusDays(4),
        location = firstOcc.location,
      ),
    )

    val response = getTapAuthorisation(auth.id).successResponse<TapAuthorisation>()
    response.verifyAgainst(auth)
    assertThat(response.locations).hasSize(1)
    firstOcc.verifyAgainst(response.occurrences.first())
  }

  @Test
  fun `200 ok finds tap authorisation and filters occurrences by date`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusHours(1),
        end = LocalDateTime.now().plusHours(3),
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(2).plusHours(1),
        end = LocalDateTime.now().plusDays(2).plusHours(3),
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(3).plusHours(1),
        end = LocalDateTime.now().plusDays(3).plusHours(3),
      ),
    )
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(4).plusHours(1),
        end = LocalDateTime.now().plusDays(4).plusHours(3),
      ),
    )

    val response = getTapAuthorisation(
      auth.id,
      LocalDate.now().plusDays(2),
      LocalDate.now().plusDays(3),
    ).successResponse<TapAuthorisation>()
    response.verifyAgainst(auth)
    assertThat(response.occurrences).hasSize(2)
    assertThat(response.totalOccurrenceCount).isEqualTo(4)
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

    val response = getTapAuthorisation(auth.id).successResponse<TapAuthorisation>()
    assertThat(response.absenceType?.code).isEqualTo("PP")
    assertThat(response.absenceSubType).isNull()
    assertThat(response.absenceReasonCategory).isNull()
    assertThat(response.absenceReason).isNull()
    response.verifyAgainst(auth)
    val occ = response.occurrences.first()
    occurrence.verifyAgainst(occ)
    assertThat(occ.absenceType?.code).isEqualTo("PP")
    assertThat(occ.absenceSubType).isNull()
    assertThat(occ.absenceReasonCategory).isNull()
    assertThat(occ.absenceReason).isNull()
  }

  private fun getTapAuthorisation(
    id: UUID,
    start: LocalDate? = null,
    end: LocalDate? = null,
    role: String? = listOf(Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW).random(),
  ) = webTestClient
    .get()
    .uri { builder ->
      builder.path(GET_TAP_AUTH_URL)
      start?.also { builder.queryParam("start", it) }
      end?.also { builder.queryParam("end", it) }
      builder.build(id)
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_TAP_AUTH_URL = "/temporary-absence-authorisations/{id}"
  }
}
