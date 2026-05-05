package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationResponses
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class IntegrationEndpointIntTests(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @ParameterizedTest
  @MethodSource("allUrls")
  fun `401 unauthorised without a valid token`(url: String) {
    webTestClient
      .get()
      .uri(url, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @MethodSource("allUrls")
  fun `403 forbidden without valid role`(url: String) {
    getIntegrationData(
      url,
      role = listOf(Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW, Roles.EXTERNAL_MOVEMENTS_UI).random(),
    )
  }

  @Test
  fun `can retrieve an authorisation`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val res = getIntegrationData(AUTH_URL, auth.id).successResponse<IntegrationResponse<IntegrationAuthorisation>>()
    res.data verifyAgainst auth
    assertThat(res.previousUrl).isNull()
    assertThat(res.nextUrl).isEqualTo(BASE_TEST_URL + (AUTH_OCC_URL from auth.id))
  }

  @Test
  fun `can retrieve authorisation occurrences`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, start = LocalDateTime.now()))
    val occ2 =
      givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth, start = LocalDateTime.now().plusDays(1)))
    val res = getIntegrationData(AUTH_OCC_URL, auth.id).successResponse<IntegrationResponses<IntegrationOccurrence>>()
    with(res.data.first()) {
      data verifyAgainst occ1
      assertThat(previousUrl).isEqualTo(BASE_TEST_URL + (AUTH_URL from auth.id))
      assertThat(nextUrl).isEqualTo(BASE_TEST_URL + (OCC_MOV_URL from occ1.id))
    }
    with(res.data.last()) {
      data verifyAgainst occ2
      assertThat(previousUrl).isEqualTo(BASE_TEST_URL + (AUTH_URL from auth.id))
      assertThat(nextUrl).isEqualTo(BASE_TEST_URL + (OCC_MOV_URL from occ2.id))
    }
    assertThat(res.previousUrl).isEqualTo(BASE_TEST_URL + (AUTH_URL from auth.id))
  }

  @Test
  fun `can retrieve an occurrence`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val res = getIntegrationData(OCC_URL, occ.id).successResponse<IntegrationResponse<IntegrationOccurrence>>()
    res.data verifyAgainst occ
    assertThat(res.previousUrl).isEqualTo(BASE_TEST_URL + (AUTH_URL from auth.id))
    assertThat(res.nextUrl).isEqualTo(BASE_TEST_URL + (OCC_MOV_URL from occ.id))
  }

  @Test
  fun `can retrieve occurrence movements`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.OUT,
            auth.person.identifier,
            occurredAt = LocalDateTime.now().minusHours(8),
          ),
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.IN,
            auth.person.identifier,
            occurredAt = LocalDateTime.now().minusHours(2),
          ),
        ),
      ),
    )
    val mov1 = occ.movements().first()
    val mov2 = occ.movements().last()
    val res = getIntegrationData(OCC_MOV_URL, occ.id).successResponse<IntegrationResponses<IntegrationMovement>>()
    with(res.data.first()) {
      data verifyAgainst mov1
      assertThat(previousUrl).isEqualTo(BASE_TEST_URL + (OCC_URL from occ.id))
      assertThat(nextUrl).isNull()
    }
    with(res.data.last()) {
      data verifyAgainst mov2
      assertThat(previousUrl).isEqualTo(BASE_TEST_URL + (OCC_URL from occ.id))
      assertThat(nextUrl).isNull()
    }
    assertThat(res.previousUrl).isEqualTo(BASE_TEST_URL + (OCC_URL from occ.id))
  }

  @Test
  fun `can retrieve a scheduled movement`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        movements = listOf(
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.OUT,
            auth.person.identifier,
          ),
        ),
      ),
    )
    val mov = occ.movements().first()
    val res = getIntegrationData(MOV_URL, mov.id).successResponse<IntegrationResponse<IntegrationMovement>>()
    res.data verifyAgainst mov
    assertThat(res.previousUrl).isEqualTo(BASE_TEST_URL + (OCC_URL from occ.id))
    assertThat(res.nextUrl).isNull()
  }

  @Test
  fun `can retrieve an unscheduled movement`() {
    val mov = givenTemporaryAbsenceMovement(temporaryAbsenceMovement(TemporaryAbsenceMovement.Direction.OUT))
    val res = getIntegrationData(MOV_URL, mov.id).successResponse<IntegrationResponse<IntegrationMovement>>()
    res.data verifyAgainst mov
    assertThat(res.previousUrl).isNull()
    assertThat(res.nextUrl).isNull()
  }

  private fun getIntegrationData(
    url: String,
    id: UUID = newUuid(),
    role: String? = listOf(Roles.EXTERNAL_MOVEMENTS_RO, Roles.EXTERNAL_MOVEMENTS_RW).random(),
  ) = webTestClient
    .get()
    .uri(url, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    private const val BASE_TEST_URL = "http://local-api/"
    private const val AUTH_URL = "integrations/temporary-absence-authorisations/{id}"
    private const val AUTH_OCC_URL = "integrations/temporary-absence-authorisations/{id}/occurrences"
    private const val OCC_URL = "integrations/temporary-absence-occurrences/{id}"
    private const val OCC_MOV_URL = "integrations/temporary-absence-occurrences/{id}/movements"
    private const val MOV_URL = "integrations/temporary-absence-movements/{id}"

    @JvmStatic
    fun allUrls() = listOf(AUTH_URL, AUTH_OCC_URL, OCC_URL, OCC_MOV_URL, MOV_URL)

    private infix fun IntegrationAuthorisation.verifyAgainst(auth: TemporaryAbsenceAuthorisation) {
      assertThat(id).isEqualTo(auth.id)
      assertThat(personIdentifier).isEqualTo(auth.person.identifier)
      assertThat(prisonCode).isEqualTo(auth.prisonCode)
      assertThat(status.code to status.description).isEqualTo(auth.status.code to auth.status.description)
      assertThat(transport.code to transport.description).isEqualTo(auth.transport.code to auth.transport.description)
      assertThat(accompaniedBy.code to accompaniedBy.description).isEqualTo(auth.accompaniedBy.code to auth.accompaniedBy.description)
      assertThat(reason.code to reason.description).isEqualTo(auth.absenceReason.code to auth.absenceReason.description)
      assertThat(repeat).isEqualTo(auth.repeat)
      assertThat(start).isEqualTo(auth.start)
      assertThat(end).isEqualTo(auth.end)
      assertThat(comments).isEqualTo(auth.comments)
    }

    private infix fun IntegrationOccurrence.verifyAgainst(occ: TemporaryAbsenceOccurrence) {
      assertThat(id).isEqualTo(occ.id)
      assertThat(personIdentifier).isEqualTo(occ.person.identifier)
      assertThat(prisonCode).isEqualTo(occ.prisonCode)
      assertThat(status.code to status.description).isEqualTo(occ.status.code to occ.status.description)
      assertThat(transport.code to transport.description).isEqualTo(occ.transport.code to occ.transport.description)
      assertThat(accompaniedBy.code to accompaniedBy.description).isEqualTo(occ.accompaniedBy.code to occ.accompaniedBy.description)
      assertThat(reason.code to reason.description).isEqualTo(occ.absenceReason.code to occ.absenceReason.description)
      assertThat(start.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(occ.start.truncatedTo(ChronoUnit.SECONDS))
      assertThat(end.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(occ.end.truncatedTo(ChronoUnit.SECONDS))
      assertThat(comments).isEqualTo(occ.comments)
    }

    private infix fun IntegrationMovement.verifyAgainst(mov: TemporaryAbsenceMovement) {
      assertThat(id).isEqualTo(mov.id)
      assertThat(occurrenceId).isEqualTo(mov.occurrence?.id)
      assertThat(personIdentifier).isEqualTo(mov.person.identifier)
      assertThat(prisonCode).isEqualTo(mov.prisonCode)
      assertThat(direction).isEqualTo(mov.direction)
      assertThat(reason.code to reason.description).isEqualTo(mov.absenceReason.code to mov.absenceReason.description)
      assertThat(accompaniedBy.code to accompaniedBy.description).isEqualTo(mov.accompaniedBy.code to mov.accompaniedBy.description)
      assertThat(accompaniedByComments).isEqualTo(mov.accompaniedByComments)
      assertThat(occurredAt.truncatedTo(ChronoUnit.SECONDS)).isEqualTo(mov.occurredAt.truncatedTo(ChronoUnit.SECONDS))
      assertThat(comments).isEqualTo(mov.comments)
    }

    private infix fun String.from(id: UUID): String = replace("{id}", id.toString())
  }
}
