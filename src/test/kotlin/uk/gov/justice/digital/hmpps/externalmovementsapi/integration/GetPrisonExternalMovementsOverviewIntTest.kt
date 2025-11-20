package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview
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
    assertThat(response.tapOverview).isEqualTo(PrisonExternalMovementOverview.TapOverview(0, 0, 0, 0))
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
        status = TapAuthorisationStatus.Code.PENDING,
      ),
    )

    val response = getPrisonEmOverview(prisonCode).successResponse<PrisonExternalMovementOverview>()

    assertThat(response.configuration).isEqualTo(PrisonExternalMovementOverview.Configuration.DEFAULT)
    assertThat(response.tapOverview).isEqualTo(
      PrisonExternalMovementOverview.TapOverview(
        leavingToday = 2,
        returningToday = 1,
        leavingNextSevenDays = 4,
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
          fromDate = LocalDate.now(),
          toDate = LocalDate.now().plusDays(1),
        ),
      )
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          authorisation,
          releaseAt = LocalDateTime.now(),
          returnBy = LocalDateTime.now().plusDays((idx % 2).toLong()),
        ),
      )
    }
  }

  private fun givenLeavingInSevenDays(prisonCode: String, vararg personIdentifiers: String) {
    val dateRange = (1L..7L).map { LocalDate.now().plusDays(it) }
    val from = dateRange.random()
    val to = from.plusDays(1)
    personIdentifiers.forEachIndexed { idx, personIdentifier ->
      val authorisation = givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          personIdentifier,
          fromDate = from,
          toDate = to,
        ),
      )
      givenTemporaryAbsenceOccurrence(
        temporaryAbsenceOccurrence(
          authorisation,
          releaseAt = LocalDateTime.of(from, LocalTime.now()),
          returnBy = LocalDateTime.of(to, LocalTime.now()),
        ),
      )
    }
  }

  private fun givenApprovalsRequired(prisonCode: String, vararg personIdentifiers: String) {
    personIdentifiers.forEachIndexed { idx, personIdentifier ->
      val from = LocalDate.now().plusDays(idx.toLong())
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          prisonCode,
          personIdentifier,
          status = TapAuthorisationStatus.Code.PENDING,
          fromDate = from,
          toDate = from.plusDays(1),
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
