package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
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

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    searchScheduleMovements(
      prisonCode(),
      role = role,
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

  @Test
  fun `200 ok - can find scheduled movements by date showing location`() {
    val prisonToFind = prisonCode()
    val start = LocalDateTime.now()
    val end = LocalDateTime.now().plusHours(4)
    val auth0 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonToFind))
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth0,
        start = start.minusDays(1),
        end = end.minusDays(1),
      ),
    )
    val auth1 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonToFind))
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1, start = start, end = end))

    val res = searchScheduleMovements(prisonToFind, includeLocation = true).successResponse<ScheduledMovements>()
    assertThat(res.content).hasSize(1)
    res.content.first().verifyAgainst(occ1)
    assertThat(res.content.map { it.location }.single()).isEqualTo(occ1.location.toString())
  }

  @Test
  fun `200 ok - can find scheduled movements by person identifier`() {
    val prisonCode = prisonCode()
    val auth1 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        accompaniedByCode = AccompaniedBy.Code.UNACCOMPANIED.value,
      ),
    )
    val toFind = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1))
    val auth2 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2))

    val res = searchScheduleMovements(
      prisonCode,
      start = toFind.start.truncatedTo(ChronoUnit.MINUTES),
      end = toFind.end.truncatedTo(ChronoUnit.MINUTES),
      personIdentifiers = listOf(auth1.person.identifier),
    ).successResponse<ScheduledMovements>()
    assertThat(res.content).hasSize(1)
    res.content.first().verifyAgainst(toFind)
  }

  @Test
  fun `200 ok - can find sensitive scheduled movements`() {
    val prisonCode = prisonCode()
    val auth1 = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        prisonCode,
        accompaniedByCode = AccompaniedBy.Code.UNACCOMPANIED.value,
      ),
    )
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth1))
    val auth2 = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    val occ2 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth2))

    val res1 = searchScheduleMovements(
      prisonCode,
      start = occ1.start.truncatedTo(ChronoUnit.MINUTES),
      end = occ1.end.truncatedTo(ChronoUnit.MINUTES),
    ).successResponse<ScheduledMovements>()
    assertThat(res1.content).hasSize(1)
    res1.content.first().verifyAgainst(occ1)

    val res2 = searchScheduleMovements(
      prisonCode,
      start = occ1.start.truncatedTo(ChronoUnit.MINUTES),
      end = occ1.end.truncatedTo(ChronoUnit.MINUTES),
      includeSensitive = true,
    ).successResponse<ScheduledMovements>()
    assertThat(res2.content).hasSize(2)
    res2.content.single { it.id == occ2.id }.verifyAgainst(occ2)
  }

  private fun ScheduledMovement.verifyAgainst(tao: TemporaryAbsenceOccurrence) {
    assertThat(id).isEqualTo(tao.id)
    assertThat(personIdentifier).isEqualTo(tao.person.identifier)
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
    personIdentifiers: List<String> = listOf(),
    includeLocation: Boolean = false,
    includeSensitive: Boolean = false,
    role: String? = Roles.EXTERNAL_MOVEMENTS_RO,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_EM_URL)
      start?.also { uri.queryParam("start", ISO_DATE_TIME.format(it)) }
      end?.also { uri.queryParam("end", ISO_DATE_TIME.format(it)) }
      personIdentifiers.takeIf { it.isNotEmpty() }?.also { uri.queryParam("personIdentifiers", it) }
      if (includeLocation) {
        uri.queryParam("includeLocation", true)
      }
      if (includeSensitive) {
        uri.queryParam("includeSensitive", true)
      }
      uri.build(prisonCode)
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_EM_URL = "/search/prisons/{prisonCode}/external-movements/schedules"
  }
}
