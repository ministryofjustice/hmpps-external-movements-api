package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPaused
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsencePaused
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.PauseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.TapAuthorisationModifications.Companion.NOT_YET_APPROVED
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PauseTapAuthorisationIntTest(
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
    pauseAuthorisation(
      newUuid(),
      pauseAuthorisationRequest(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    pauseAuthorisation(newUuid(), pauseAuthorisationRequest()).expectStatus().isNotFound
  }

  @ParameterizedTest
  @EnumSource(AuthorisationStatus.Code::class, mode = EXCLUDE, names = ["APPROVED", "PAUSED"])
  fun `409 - authorisation not approved cannot be paused`(status: AuthorisationStatus.Code) {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = status))
    val res = pauseAuthorisation(auth.id, pauseAuthorisationRequest()).errorResponse(HttpStatus.CONFLICT)
    assertThat(res.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(res.userMessage).isEqualTo(NOT_YET_APPROVED)
  }

  @Test
  fun `200 ok - authorisation paused`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = true))
    val pastOccurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().minusDays(3),
        end = LocalDateTime.now().minusDays(2),
      ),
    )
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val username = username()
    val caseloadId = word(5)
    val request = pauseAuthorisationRequest()

    val res = pauseAuthorisation(auth.id, request, username, caseloadId).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationPaused.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("status", "Approved", "Paused"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.PAUSED.name)
    val previousAbsence = requireNotNull(findTemporaryAbsenceOccurrence(pastOccurrence.id))
    assertThat(previousAbsence.status.code).isEqualTo(OccurrenceStatus.Code.EXPIRED.name)
    assertThat(previousAbsence.dpsOnly).isFalse
    val absence = requireNotNull(findTemporaryAbsenceOccurrence(occurrence.id))
    assertThat(absence.status.code).isEqualTo(OccurrenceStatus.Code.PAUSED.name)
    assertThat(absence.dpsOnly).isTrue

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username, reason = request.reason, caseloadId = caseloadId),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationPaused(auth.person.identifier, auth.id).publication(auth.id),
        TemporaryAbsencePaused(auth.person.identifier, occurrence.id).publication(occurrence.id),
      ),
    )
  }

  @Test
  fun `200 ok - single authorisation paused`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        repeat = false,
        start = LocalDate.now().plusDays(1),
        end = LocalDate.now().plusDays(1),
      ),
    )
    val occ = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(1),
        end = LocalDateTime.now().plusDays(1),
      ),
    )
    val request = pauseAuthorisationRequest()
    val username = username()
    val caseloadId = word(4)
    val res = pauseAuthorisation(auth.id, request, username, caseloadId).successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationPaused.EVENT_TYPE)
    assertThat(res.reason).isEqualTo(request.reason)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change("status", "Approved", "Paused"),
    )

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.PAUSED.name)
    val occurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ.id))
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.PAUSED.name)
    assertThat(occurrence.dpsOnly).isTrue

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username, reason = request.reason, caseloadId = caseloadId),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationPaused(auth.person.identifier, auth.id).publication(auth.id),
        TemporaryAbsencePaused(auth.person.identifier, occurrence.id).publication(occurrence.id),
      ),
    )
  }

  private fun pauseAuthorisationRequest(
    reason: String? = word(20),
  ) = PauseAuthorisation(reason)

  private fun pauseAuthorisation(
    id: UUID,
    request: PauseAuthorisation,
    username: String = DEFAULT_USERNAME,
    caseloadId: String? = null,
    role: String? = Roles.TEMPORARY_ABSENCE_RW,
  ) = webTestClient
    .put()
    .uri(TAP_AUTHORISATION_MODIFICATION_URL, id)
    .bodyValue(request)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .headers { h -> caseloadId?.also { h.put(CaseloadIdHeader.NAME, listOf(it)) } }
    .exchange()

  companion object {
    const val TAP_AUTHORISATION_MODIFICATION_URL = "/temporary-absence-authorisations/{id}"
  }
}
