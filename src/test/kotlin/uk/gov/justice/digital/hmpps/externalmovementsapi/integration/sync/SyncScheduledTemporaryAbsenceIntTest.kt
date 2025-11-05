package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.NomisAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.ScheduledTemporaryAbsenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapLocation
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SyncScheduledTemporaryAbsenceIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC_SCHEDULED_TAP_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    syncScheduledTemporaryAbsence(
      newUuid(),
      scheduledAbsenceRequest(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `200 ok scheduled absence created successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = scheduledAbsenceRequest(
      eventStatus = "PEN",
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncScheduledTemporaryAbsence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.cancelledAt).isNull()
    assertThat(saved.cancelledBy).isNull()

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceRescheduled(authorisation.personIdentifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok scheduled absence updated successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val existing = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val request = scheduledAbsenceRequest(id = existing.id, eventId = existing.legacyId!!, eventStatus = "CANC")
    val res = syncScheduledTemporaryAbsence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(existing.id))
    saved.verifyAgainst(request)
    assertThat(saved.cancelledAt).isNotNull
    assertThat(saved.cancelledBy).isNotNull

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok scheduled absence id returned if legacy id already exists`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val existing = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val request = scheduledAbsenceRequest(eventId = existing.legacyId!!, eventStatus = "DEN", escortCode = "U")
    val res = syncScheduledTemporaryAbsence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(existing.id))
    saved.verifyAgainst(request)
    assertThat(saved.cancelledAt).isNotNull
    assertThat(saved.cancelledBy).isNotNull

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok scheduled absence created if one doesn't exist with the given uuid`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = scheduledAbsenceRequest(id = newUuid())
    val res = syncScheduledTemporaryAbsence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(request.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceRescheduled(authorisation.personIdentifier, saved.id, DataSource.NOMIS)))
  }

  private fun scheduledAbsenceRequest(
    id: UUID? = null,
    eventStatus: String = "SCH",
    startTime: LocalDateTime = LocalDateTime.now().minusDays(7),
    returnTime: LocalDateTime = LocalDateTime.now(),
    escortCode: String? = "L",
    transportType: String? = "OD",
    location: TapLocation = TapLocation(description = name(10)),
    comment: String? = "Some notes about the absence",
    contactPersonName: String? = null,
    eventId: Long = newId(),
    audit: NomisAudit = NomisAuditGenerator.generate(),
  ) = ScheduledTemporaryAbsenceRequest(
    id,
    eventId,
    eventStatus,
    startTime,
    returnTime,
    location,
    contactPersonName,
    escortCode,
    comment,
    transportType,
    audit,
  )

  private fun syncScheduledTemporaryAbsence(
    parentId: UUID,
    request: ScheduledTemporaryAbsenceRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC_SCHEDULED_TAP_URL, parentId)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC_SCHEDULED_TAP_URL = "/sync/scheduled-temporary-absence/{parentId}"
  }
}

private fun TemporaryAbsenceOccurrence.verifyAgainst(
  request: ScheduledTemporaryAbsenceRequest,
) {
  assertThat(releaseAt).isCloseTo(request.startTime, within(2, SECONDS))
  assertThat(returnBy).isCloseTo(request.returnTime, within(2, SECONDS))
  assertThat(accompaniedBy.code).isEqualTo(request.escort)
  assertThat(transport.code).isEqualTo(request.transportType)
  assertThat(location).isEqualTo(request.location.asLocation())
  assertThat(notes).isEqualTo(request.comment)
  assertThat(legacyId).isEqualTo(request.eventId)
  assertThat(addedBy).isEqualTo(request.audit.createUsername)
  assertThat(addedAt).isCloseTo(request.audit.createDatetime, within(2, SECONDS))
}
