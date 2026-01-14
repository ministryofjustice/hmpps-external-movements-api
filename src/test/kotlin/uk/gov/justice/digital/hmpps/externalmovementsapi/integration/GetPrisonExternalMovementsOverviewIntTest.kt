package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview.TapOverview
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GetPrisonExternalMovementsOverviewIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_PRISON_EM_OVERVIEW_URL, prisonCode())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getPrisonEmOverview(prisonCode(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `200 ok - default returned when no config`() {
    val prisonCode = prisonCode()
    val response = getPrisonEmOverview(prisonCode).successResponse<PrisonExternalMovementOverview>()

    assertThat(response.configuration).isEqualTo(PrisonExternalMovementOverview.Configuration.DEFAULT)
    assertThat(response.tapOverview).isEqualTo(TapOverview(0, 0, 0))
  }

  @Test
  fun `200 ok - counts are correctly returned`() {
    val prisonCode = prisonCode()
    givenLeavingToday(prisonCode, personIdentifier(), personIdentifier())
    givenLeavingInSevenDays(prisonCode, personIdentifier(), personIdentifier(), personIdentifier(), personIdentifier())
    givenApprovalsRequired(prisonCode, personIdentifier(), personIdentifier(), personIdentifier())
    givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        status = AuthorisationStatus.Code.PENDING,
        end = LocalDate.now().minusDays(1),
      ),
    )

    val response = getPrisonEmOverview(prisonCode).successResponse<PrisonExternalMovementOverview>()

    assertThat(response.configuration).isEqualTo(PrisonExternalMovementOverview.Configuration.DEFAULT)
    assertThat(response.tapOverview).isEqualTo(
      TapOverview(
        leavingToday = 2,
        returningToday = 1,
        approvalsRequired = 3,
      ),
    )
  }

  private fun givenLeavingToday(prisonCode: String, vararg personIdentifiers: String) {
    personIdentifiers.forEachIndexed { idx, personIdentifier ->
      val authorisation = givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          personIdentifier,
          start = LocalDate.now(),
          end = LocalDate.now().plusDays(1),
        ),
      )
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          authorisation,
          start = LocalDateTime.now(),
          end = LocalDateTime.now().plusDays((idx % 2).toLong()),
        ),
      )
    }
  }

  private fun givenLeavingInSevenDays(prisonCode: String, vararg personIdentifiers: String) {
    val dateRange = (1L..7L).map { LocalDate.now().plusDays(it) }
    val start = dateRange.random()
    val end = start.plusDays(1)
    personIdentifiers.forEach { personIdentifier ->
      val authorisation = givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          personIdentifier,
          start = start,
          end = end,
        ),
      )
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          authorisation,
          start = LocalDateTime.of(start, LocalTime.now()),
          end = LocalDateTime.of(end, LocalTime.now()),
        ),
      )
    }
  }

  private fun givenApprovalsRequired(prisonCode: String, vararg personIdentifiers: String) {
    personIdentifiers.forEachIndexed { idx, personIdentifier ->
      val start = LocalDate.now().plusDays(idx.toLong())
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          personIdentifier,
          status = AuthorisationStatus.Code.PENDING,
          start = start,
          end = start.plusDays(1),
        ),
      )
    }
  }

  private fun getPrisonEmOverview(
    prisonCode: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_PRISON_EM_OVERVIEW_URL, prisonCode)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_PRISON_EM_OVERVIEW_URL = "/prisons/{prisonIdentifier}/external-movements/overview"
  }
}
