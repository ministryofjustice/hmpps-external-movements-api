package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovementDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovementType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovements
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.temporal.ChronoUnit

class SearchScheduledMovementsIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(SEARCH_EM_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    searchScheduleMovements(
      prisonCode(),
      role = "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `200 ok - can find scheduled movements defaults`() {
    val prisonToFind = prisonCode()
    val start = LocalDateTime.now()
    val end = LocalDateTime.now().plusHours(4)
    val wrongPrisonAuth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(wrongPrisonAuth, start = start, end = end))
    val wrongDate = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonToFind))
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        wrongDate,
        start = start.minusDays(1),
        end = end.minusDays(1),
      ),
    )
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonToFind))
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1, start = start, end = end))

    val res = searchScheduleMovements(prisonToFind).successResponse<ScheduledMovements>()
    assertThat(res.content).hasSize(1)
    res.content.first().verifyAgainst(occ1)
    assertThat(res.content.map { it.location }.single()).isEqualTo("External")
  }

  private fun ScheduledMovement.verifyAgainst(tao: TemporaryAbsenceOccurrence) {
    assertThat(id).isEqualTo(tao.id)
    assertThat(personIdentifier).isEqualTo(tao.authorisation.person.identifier)
    assertThat(domain).isEqualTo(
      CodedDescription(
        ScheduledMovementDomain.EXTERNAL_MOVEMENTS.name,
        ScheduledMovementDomain.EXTERNAL_MOVEMENTS.description,
      ),
    )
    assertThat(type).isEqualTo(
      CodedDescription(
        ScheduledMovementType.TEMPORARY_ABSENCE.name,
        ScheduledMovementType.TEMPORARY_ABSENCE.description,
      ),
    )
    assertThat(status.code).isEqualTo(tao.status.code)
    assertThat(start).isCloseTo(tao.start, within(10, ChronoUnit.SECONDS))
    assertThat(end).isCloseTo(tao.end, within(10, ChronoUnit.SECONDS))
    assertThat(detail.uiUrl).isEqualTo("https://local-ui/temporary-absences/$id")
  }

  private fun searchScheduleMovements(
    prisonCode: String,
    start: LocalDateTime? = null,
    end: LocalDateTime? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_RO,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_EM_URL)
      start?.also { uri.queryParam("start", ISO_DATE_TIME.format(it)) }
      end?.also { uri.queryParam("end", ISO_DATE_TIME.format(it)) }
      uri.build(prisonCode)
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_EM_URL = "/search/prisons/{prisonCode}/external-movements/schedules"
  }
}
