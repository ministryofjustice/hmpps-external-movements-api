package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.NomisAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.ScheduledTemporaryAbsenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.SyncResponse
import java.time.LocalDateTime
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
  }

  @Test
  fun `200 ok scheduled absence created if one doesn't exist with the given uuid`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = scheduledAbsenceRequest(id = newUuid(), toAddressOwnerClass = "CORP")
    val res = syncScheduledTemporaryAbsence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(request.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
  }

  private fun scheduledAbsenceRequest(
    id: UUID? = null,
    eventStatus: String = "SCH",
    startTime: LocalDateTime = LocalDateTime.now().minusDays(7),
    returnTime: LocalDateTime = LocalDateTime.now(),
    toAddressOwnerClass: String? = "OFF",
    toAddressId: Long? = newId(),
    escortCode: String? = "L",
    transportType: String? = "OD",
    comment: String? = "Some notes about the application",
    contactPersonName: String? = null,
    eventId: Long = newId(),
    audit: NomisAudit = NomisAuditGenerator.generate(),
  ) = ScheduledTemporaryAbsenceRequest(
    id,
    eventId,
    eventStatus,
    startTime,
    returnTime,
    toAddressOwnerClass,
    toAddressId,
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
