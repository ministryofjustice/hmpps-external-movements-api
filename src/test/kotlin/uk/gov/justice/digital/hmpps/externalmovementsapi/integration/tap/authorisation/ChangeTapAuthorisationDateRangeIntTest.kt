package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.UUID

class ChangeTapAuthorisationDateRangeIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(TAP_AUTHORISATION_MODIFICATION_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    changeDateRange(
      newUuid(),
      changeDateRange(),
      role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    changeDateRange(newUuid(), changeDateRange()).expectStatus().isNotFound
  }

  @Test
  fun `400 bad request - date range over 6 months`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val start = LocalDate.now()
    val end = start.plusMonths(6).plusDays(1)
    val response = changeDateRange(auth.id, changeDateRange(start, end)).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(response.userMessage).isEqualTo("Validation failure: The authorisation date range must not be more than 6 months")
  }

  @Test
  fun `400 bad request - attempt to change date range to be shorter than range of occurrences`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occ1 = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val occ2 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(3),
        end = LocalDateTime.now().plusDays(4),
      ),
    )
    val response = changeDateRange(
      auth.id,
      changeDateRange(occ1.start.plusDays(1).toLocalDate(), occ2.end.minusDays(1).toLocalDate()),
    ).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(response.userMessage).isEqualTo("Validation failure: Authorisation date range cannot be less than the date range of absences")
  }

  @Test
  fun `200 ok - authorisation date range updated`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = changeDateRange(occ.start.toLocalDate(), occ.end.toLocalDate())
    val res = changeDateRange(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationDateRangeChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("start", ISO_DATE.format(auth.start), ISO_DATE.format(request.start)),
      AuditedAction.Change("end", ISO_DATE.format(auth.end), ISO_DATE.format(request.end)),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, reason = request.reason),
    )

    verifyEvents(
      saved,
      setOf(TemporaryAbsenceAuthorisationDateRangeChanged(auth.person.identifier, auth.id)),
    )
  }

  @Test
  fun `200 ok - no-op date range change request`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    val request = changeDateRange(auth.start, auth.end)
    val res = changeDateRange(auth.id, request).successResponse<AuditHistory>()
    assertThat(res.content).isEmpty()

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get(),
    )
  }

  private fun changeDateRange(
    from: LocalDate = LocalDate.now().plusDays(1),
    to: LocalDate = LocalDate.now().plusDays(2),
    reason: String? = "Reason for changing the date range",
  ) = ChangeAuthorisationDateRange(from, to, reason)

  private fun changeDateRange(
    id: UUID,
    request: ChangeAuthorisationDateRange,
    role: String? = Roles.TEMPORARY_ABSENCE_RW,
  ) = webTestClient
    .put()
    .uri(TAP_AUTHORISATION_MODIFICATION_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val TAP_AUTHORISATION_MODIFICATION_URL = "/temporary-absence-authorisations/{id}"
  }
}
