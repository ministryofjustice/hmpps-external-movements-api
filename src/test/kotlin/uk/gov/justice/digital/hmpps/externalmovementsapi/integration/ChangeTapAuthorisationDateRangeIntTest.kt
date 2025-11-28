package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import java.time.LocalDate
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
      .post()
      .uri(TAP_AUTHORISATION_MODIFICATION_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    changeDateRange(
      newUuid(),
      changeDateRange(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    changeDateRange(newUuid(), changeDateRange()).expectStatus().isNotFound
  }

  @Test
  fun `200 ok - authorisation date range updated`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = PENDING))
    givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = changeDateRange()
    val res = changeDateRange(auth.id, request).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationDateRangeChanged.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("fromDate", ISO_DATE.format(auth.fromDate), ISO_DATE.format(request.from)),
      AuditedAction.Change("toDate", ISO_DATE.format(auth.toDate), ISO_DATE.format(request.to)),
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
    val request = changeDateRange(auth.fromDate, auth.toDate)
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
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
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
