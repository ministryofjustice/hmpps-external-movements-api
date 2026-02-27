package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCancelled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapOccurrence
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SyncTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
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

  @ParameterizedTest
  @EnumSource(AuthorisationStatus.Code::class, mode = EXCLUDE, names = ["APPROVED"])
  fun `409 conflict attempting to add a new occurrence to a non-active authorisation`(statusCode: AuthorisationStatus.Code) {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(status = statusCode))
    val request = tapOccurrence()
    val res = syncTapOccurrence(authorisation.id, request).errorResponse(HttpStatus.CONFLICT)

    assertThat(res.userMessage).isEqualTo("Attempt to add occurrence to a non-approved authorisation")
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
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceScheduled(authorisation.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
        TemporaryAbsenceAuthorisationRelocated(
          authorisation.person.identifier,
          authorisation.id,
          DataSource.NOMIS,
        ).publication(authorisation.id),
      ),
    )
  }

  @Test
  fun `200 ok scheduled absence updated successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val existing = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val request = tapOccurrence(
      id = existing.id,
      isCancelled = true,
      legacyId = existing.legacyId!!,
      updated = AtAndBy(LocalDateTime.now().minusMinutes(20), SYSTEM_USERNAME),
      location = existing.location,
    )

    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(existing.id))
    saved.verifyAgainst(request)
    assertThat(saved.status.code).isEqualTo(OccurrenceStatus.Code.CANCELLED.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(
        TemporaryAbsenceRescheduled(saved.authorisation.person.identifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceCancelled(saved.authorisation.person.identifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceCommentsChanged(saved.authorisation.person.identifier, saved.id, DataSource.NOMIS),
      ),
    )
  }

  @Test
  fun `200 ok scheduled absence updated - updating location on movements`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val existing = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authorisation,
        legacyId = newId(),
        movements = listOf(
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.OUT,
            authorisation.person.identifier,
          ),
          temporaryAbsenceMovement(
            TemporaryAbsenceMovement.Direction.IN,
            authorisation.person.identifier,
          ),
        ),
      ),
    )
    val request = tapOccurrence(
      id = existing.id,
      legacyId = existing.legacyId!!,
      updated = AtAndBy(LocalDateTime.now().minusMinutes(10), SYSTEM_USERNAME),
    )

    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(existing.id))
    saved.verifyAgainst(request)
    assertThat(saved.status.code).isEqualTo(OccurrenceStatus.Code.COMPLETED.name)
    assertThat(saved.movements().map { it.location }).containsOnly(request.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceMovement::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS, reason = null),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceRescheduled(
          saved.authorisation.person.identifier,
          saved.id,
          DataSource.NOMIS,
        ).publication(saved.id),
        TemporaryAbsenceRelocated(
          saved.authorisation.person.identifier,
          saved.id,
          DataSource.NOMIS,
        ).publication(saved.id),
        TemporaryAbsenceCommentsChanged(saved.authorisation.person.identifier, saved.id, DataSource.NOMIS).publication(
          saved.id,
        ),
        TemporaryAbsenceAuthorisationRelocated(
          saved.authorisation.person.identifier,
          authorisation.id,
          DataSource.NOMIS,
        ).publication(authorisation.id),
      ) + saved.movements().map {
        TapMovementRelocated(saved.person.identifier, it.id, DataSource.NOMIS).publication(it.id)
      },
    )
  }

  @Test
  fun `200 ok can create occurrence if cancelled`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = true))
    val request = tapOccurrence(isCancelled = true)
    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.status.code).isEqualTo(OccurrenceStatus.Code.CANCELLED.name)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationRelocated(
          saved.authorisation.person.identifier,
          authorisation.id,
          DataSource.NOMIS,
        ).publication(authorisation.id),
      ),
    )
  }

  @Test
  fun `200 ok scheduled absence id returned if legacy id already exists`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        legacyId = newId(),
        locations = linkedSetOf(location()),
      ),
    )
    val existing = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authorisation,
        legacyId = newId(),
        absenceType = "PP",
        absenceSubType = "PP",
        absenceReasonCategory = null,
        absenceReason = "PC",
        location = authorisation.locations.single(),
      ),
    )
    val request = tapOccurrence(
      legacyId = existing.legacyId!!,
      accompaniedByCode = existing.accompaniedBy.code,
      start = existing.start,
      end = existing.end,
      contactInformation = null,
      location = existing.location,
      comments = existing.comments,
      typeCode = existing.absenceType?.code,
      subTypeCode = existing.absenceSubType?.code,
      reasonCode = existing.absenceReason.code,
    )
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
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(),
    )
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
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceScheduled(authorisation.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
        TemporaryAbsenceAuthorisationRelocated(
          authorisation.person.identifier,
          authorisation.id,
          DataSource.NOMIS,
        ).publication(authorisation.id),
      ),
    )
  }

  @Test
  fun `200 ok absence with path for reason only`() {
    val authorisation =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(locations = linkedSetOf(location())))
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
      setOf(
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceScheduled(authorisation.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
        TemporaryAbsenceAuthorisationRelocated(
          authorisation.person.identifier,
          authorisation.id,
          DataSource.NOMIS,
        ).publication(authorisation.id),
      ),
    )
  }

  @Test
  fun `200 ok remove duplicate if created pending in dps and approved in nomis`() {
    val authorisation =
      givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(repeat = false, legacyId = newId()))
    val dpsOccurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))
    val request = tapOccurrence(location = dpsOccurrence.location)
    val res = syncTapOccurrence(authorisation.id, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)

    // dps occurrence should be deleted to prevent a duplicate occurrence being created on a single authorisation
    assertThat(findTemporaryAbsenceOccurrence(dpsOccurrence.id)).isNull()

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceScheduled(authorisation.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 ok occurrence created for security escort`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(locations = linkedSetOf(location())))
    val request = tapOccurrence(
      reasonCode = "SE",
      typeCode = "SE",
      subTypeCode = "SE",
      location = auth.locations.single(),
    )
    val res = syncTapOccurrence(auth.id, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.absenceSubType).isNull()
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SE",
      ReferenceDataDomain.Code.ABSENCE_REASON of "SE",
    )

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceScheduled(auth.person.identifier, saved.id, DataSource.NOMIS).publication(saved.id),
      ),
    )
  }

  @Test
  fun `200 ok occurrence updated for security escort`() {
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val request = tapOccurrence(
      id = occurrence.id,
      reasonCode = "SE",
      typeCode = "SE",
      subTypeCode = "SE",
      start = occurrence.start,
      end = occurrence.end,
      location = occurrence.location,
      comments = occurrence.comments,
    )
    val res = syncTapOccurrence(auth.id, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceOccurrence(res.id))
    saved.verifyAgainst(request)
    assertThat(saved.absenceSubType).isNull()
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SE",
      ReferenceDataDomain.Code.ABSENCE_REASON of "SE",
    )

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceRecategorised(auth.person.identifier, saved.id, DataSource.NOMIS)))
  }

  private fun tapOccurrence(
    id: UUID? = null,
    isCancelled: Boolean = false,
    reasonCode: String = "R15",
    typeCode: String? = "SR",
    subTypeCode: String? = "RDR",
    start: LocalDateTime = LocalDateTime.now().minusHours(7),
    end: LocalDateTime = LocalDateTime.now().plusHours(1),
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    location: Location = location(),
    contactInformation: String? = "Contact ${word(8)}",
    comments: String? = "Some comments about the absence",
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusMonths(1), DEFAULT_USERNAME),
    updated: AtAndBy? = null,
    legacyId: Long = newId(),
  ) = TapOccurrence(
    id,
    isCancelled,
    start,
    end,
    location,
    typeCode,
    subTypeCode,
    reasonCode,
    accompaniedByCode,
    transportCode,
    contactInformation,
    comments,
    created,
    updated,
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

private fun TemporaryAbsenceOccurrence.verifyAgainst(request: TapOccurrence) {
  assertThat(start).isCloseTo(request.start, within(2, SECONDS))
  assertThat(end).isCloseTo(request.end, within(2, SECONDS))
  assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
  assertThat(transport.code).isEqualTo(request.transportCode)
  assertThat(location).isEqualTo(request.location)
  assertThat(contactInformation).isEqualTo(request.contactInformation)
  assertThat(comments).isEqualTo(request.comments)
  legacyId?.also { assertThat(it).isEqualTo(request.legacyId) }
}
