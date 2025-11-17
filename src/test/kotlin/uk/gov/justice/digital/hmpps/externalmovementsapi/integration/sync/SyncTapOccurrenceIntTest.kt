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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapOccurrence
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SyncTapOccurrenceIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC_TAP_OCCUR_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    syncTapOccurrence(
      newUuid(),
      tapOccurrence(),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `200 ok scheduled absence created successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = tapOccurrence()
    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceScheduled(authorisation.personIdentifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok scheduled absence updated successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val existing = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val request = tapOccurrence(
      id = existing.id,
      isCancelled = true,
      legacyId = existing.legacyId!!,
      cancelled = AtAndBy(LocalDateTime.now().minusMinutes(20), SYSTEM_USERNAME),
    )

    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(existing.id))
    saved.verifyAgainst(request)
    assertThat(saved.status.code).isEqualTo(TapOccurrenceStatus.Code.CANCELLED.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(
        TemporaryAbsenceRescheduled(saved.authorisation.personIdentifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceCancelled(saved.authorisation.personIdentifier, saved.id, DataSource.NOMIS),
      ),
    )
  }

  @Test
  fun `200 ok can create occurrence if cancelled`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = tapOccurrence(isCancelled = true)
    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.status.code).isEqualTo(TapOccurrenceStatus.Code.CANCELLED.name)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok scheduled absence id returned if legacy id already exists`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val existing = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val request = tapOccurrence(legacyId = existing.legacyId!!, accompaniedByCode = "U", releaseAt = existing.releaseAt, returnBy = existing.returnBy)
    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(existing.id))
    saved.verifyAgainst(request)

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
    val request = tapOccurrence(id = newUuid())
    val res = syncTapOccurrence(authorisation.id, request)
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

    verifyEvents(saved, setOf(TemporaryAbsenceScheduled(authorisation.personIdentifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok absence with path for reason only`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val request = tapOccurrence(typeCode = null, subTypeCode = null, reasonCode = "PC")
    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.reasonPath).isEqualTo(ReasonPath(listOf(ReferenceDataDomain.Code.ABSENCE_REASON of "PC")))

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceScheduled(authorisation.personIdentifier, saved.id, DataSource.NOMIS)))
  }

  private fun tapOccurrence(
    id: UUID? = null,
    isCancelled: Boolean = false,
    reasonCode: String = "R15",
    typeCode: String? = "SR",
    subTypeCode: String? = "RDR",
    releaseAt: LocalDateTime = LocalDateTime.now().minusHours(7),
    returnBy: LocalDateTime = LocalDateTime.now().plusHours(1),
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    location: Location = location(),
    notes: String? = "Some notes about the absence",
    added: AtAndBy = AtAndBy(LocalDateTime.now().minusMonths(1), DEFAULT_USERNAME),
    cancelled: AtAndBy? = null,
    legacyId: Long = newId(),
  ) = TapOccurrence(
    id,
    isCancelled,
    releaseAt,
    returnBy,
    location,
    typeCode,
    subTypeCode,
    reasonCode,
    accompaniedByCode,
    transportCode,
    notes,
    added,
    cancelled,
    legacyId,
  )

  private fun syncTapOccurrence(
    authorisationId: UUID,
    request: TapOccurrence,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC_TAP_OCCUR_URL, authorisationId)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC_TAP_OCCUR_URL = "/sync/temporary-absence-authorisations/{authorisationId}/occurrences"
  }
}

private fun TemporaryAbsenceOccurrence.verifyAgainst(
  request: TapOccurrence,
) {
  assertThat(releaseAt).isCloseTo(request.releaseAt, within(2, SECONDS))
  assertThat(returnBy).isCloseTo(request.returnBy, within(2, SECONDS))
  assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
  assertThat(transport.code).isEqualTo(request.transportCode)
  assertThat(location).isEqualTo(request.location)
  assertThat(notes).isEqualTo(request.notes)
  assertThat(legacyId).isEqualTo(request.legacyId)
}
