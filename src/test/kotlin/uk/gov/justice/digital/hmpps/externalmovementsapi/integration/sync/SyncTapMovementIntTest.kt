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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement.Direction
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOccurredAtChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementOccurrenceChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TapMovementRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceCompleted
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceStarted
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovement
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SyncTapMovementIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC_TAP_MOVEMENT_URL, newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    syncTapMovement(
      personIdentifier(),
      tapMovement(direction = Direction.OUT),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if person identifier does not match occurrence person identifier`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))
    val request = tapMovement(
      direction = Direction.IN,
      occurrenceId = occurrence.id,
      prisonCode = authorisation.prisonCode,
    )
    syncTapMovement(personIdentifier(), request).expectStatus().isBadRequest
  }

  @Test
  fun `200 ok temporary absence started successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)
    val request = tapMovement(
      direction = Direction.OUT,
      occurrenceId = occurrence.id,
      prisonCode = authorisation.prisonCode,
      location = occurrence.location,
    )
    val res = syncTapMovement(authorisation.person.identifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceMovement(res.id))
    saved.verifyAgainst(authorisation.person.identifier, request)
    assertThat(saved.occurrence?.status?.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceMovement::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(
        username = DEFAULT_USERNAME,
        source = DataSource.NOMIS,
        reason = TemporaryAbsenceMovement.formattedReason(saved),
      ),
    )
    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceStarted(
          authorisation.person.identifier,
          saved.id,
          occurrence.id,
          DataSource.NOMIS,
        ).publication(),
        TemporaryAbsenceStarted(
          authorisation.person.identifier,
          saved.id,
          occurrence.id,
          DataSource.NOMIS,
        ).publication { false },
      ),
    )
  }

  @Test
  fun `200 ok temporary absence completed successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authorisation,
        movements = listOf(temporaryAbsenceMovement(Direction.OUT, personIdentifier = authorisation.person.identifier)),
      ),
    )
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)

    val request = tapMovement(
      direction = Direction.IN,
      occurrenceId = occurrence.id,
      prisonCode = authorisation.prisonCode,
      location = occurrence.location,
    )
    val res = syncTapMovement(authorisation.person.identifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceMovement(res.id))
    saved.verifyAgainst(authorisation.person.identifier, request)
    assertThat(saved.occurrence?.status?.code).isEqualTo(OccurrenceStatus.Code.COMPLETED.name)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceMovement::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(
        username = DEFAULT_USERNAME,
        source = DataSource.NOMIS,
        reason = TemporaryAbsenceMovement.formattedReason(saved),
      ),
    )
    verifyEventPublications(
      saved,
      setOf(
        TemporaryAbsenceCompleted(
          authorisation.person.identifier,
          saved.id,
          occurrence.id,
          DataSource.NOMIS,
        ).publication(),
        TemporaryAbsenceCompleted(
          authorisation.person.identifier,
          saved.id,
          occurrence.id,
          DataSource.NOMIS,
        ).publication { false },
      ),
    )
  }

  @Test
  fun `200 ok temporary absence updated successfully including update of occurrence location`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val existing = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        Direction.OUT,
        authorisation.person.identifier,
        occurrence,
        legacyId = newId().toString(),
      ),
    )
    val request = tapMovement(
      accompaniedByCode = "U",
      accompaniedByComments = "Updated the text about the escort",
      direction = Direction.OUT,
      id = existing.id,
      occurrenceId = occurrence.id,
      legacyId = existing.legacyId!!,
      prisonCode = existing.prisonCode,
    )
    val res = syncTapMovement(authorisation.person.identifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceMovement(existing.id))
    saved.verifyAgainst(authorisation.person.identifier, request)
    assertThat(saved.location).isEqualTo(saved.occurrence!!.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceMovement::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEvents(
      saved,
      setOf(
        TapMovementAccompanimentChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TapMovementCommentsChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TapMovementOccurredAtChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TapMovementRelocated(saved.person.identifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceRelocated(saved.person.identifier, occurrence.id, DataSource.NOMIS),
        TemporaryAbsenceAuthorisationRelocated(saved.person.identifier, authorisation.id, DataSource.NOMIS),
      ),
    )
  }

  @Test
  fun `200 ok temporary absence id returned if legacy id already exists`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val existing = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        Direction.IN,
        authorisation.person.identifier,
        occurrence,
        legacyId = newId().toString(),
      ),
    )
    val request = tapMovement(
      accompaniedByCode = "U",
      accompaniedByComments = "Updated the text about the escort",
      occurredAt = existing.occurredAt.minusHours(1),
      direction = Direction.IN,
      occurrenceId = occurrence.id,
      legacyId = existing.legacyId!!,
      prisonCode = existing.prisonCode,
      location = existing.location,
    )
    val res = syncTapMovement(authorisation.person.identifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceMovement(existing.id))
    saved.verifyAgainst(authorisation.person.identifier, request)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEvents(
      saved,
      setOf(
        TapMovementAccompanimentChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TapMovementCommentsChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TapMovementOccurredAtChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
      ),
    )
  }

  @Test
  fun `200 ok absence created if one doesn't exist with the given uuid`() {
    val personIdentifier = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode(), setOf(personIdentifier))
    val request = tapMovement(id = newUuid(), direction = Direction.IN)
    val res = syncTapMovement(personIdentifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(request.id)
    val saved = requireNotNull(findTemporaryAbsenceMovement(res.id))
    saved.verifyAgainst(personIdentifier, request)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )
    verifyEvents(saved, setOf(TemporaryAbsenceCompleted(saved.person.identifier, saved.id, null, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok temporary absence movement switches scheduled occurrence`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occ1 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authorisation,
        legacyId = newId(),
        movements = listOf(
          temporaryAbsenceMovement(
            Direction.OUT,
            authorisation.person.identifier,
            legacyId = newId().toString(),
          ),
        ),
      ),
    )
    val movement = occ1.movements().first()
    assertThat(movement.occurrence!!.id).isEqualTo(occ1.id)
    assertThat(movement.occurrence!!.status.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)

    val occ2 = givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        authorisation,
        legacyId = newId(),
        location = movement.location,
      ),
    )

    val request = tapMovement(
      accompaniedByCode = movement.accompaniedBy.code,
      accompaniedByComments = movement.accompaniedByComments,
      direction = movement.direction,
      location = movement.location,
      occurredAt = movement.occurredAt,
      legacyId = movement.legacyId!!,
      comments = movement.comments,
      prisonCode = movement.prisonCode,
      id = movement.id,
      occurrenceId = occ2.id,
    )
    val res = syncTapMovement(authorisation.person.identifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(movement.id)
    val saved = requireNotNull(findTemporaryAbsenceMovement(movement.id))
    saved.verifyAgainst(authorisation.person.identifier, request)
    assertThat(saved.occurrence!!.id).isEqualTo(occ2.id)
    assertThat(saved.occurrence!!.status.code).isEqualTo(OccurrenceStatus.Code.IN_PROGRESS.name)

    val oldOccurrence = requireNotNull(findTemporaryAbsenceOccurrence(occ1.id))
    assertThat(oldOccurrence.status.code).isEqualTo(OccurrenceStatus.Code.SCHEDULED.name)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(
        TemporaryAbsenceMovement::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get()
        .copy(source = DataSource.NOMIS, reason = "Recorded movement temporary absence occurrence changed"),
    )
    verifyEventPublications(
      saved,
      setOf(
        TapMovementOccurrenceChanged(movement.person.identifier, movement.id, DataSource.NOMIS).publication { false },
        TemporaryAbsenceStarted(
          movement.person.identifier,
          movement.id,
          occ2.id,
          DataSource.NOMIS,
        ).publication { false },
      ),
    )
  }

  private fun tapMovement(
    id: UUID? = null,
    occurrenceId: UUID? = null,
    direction: Direction,
    prisonCode: String = prisonCode(),
    occurredAt: LocalDateTime = LocalDateTime.now().minusDays(7),
    reasonCode: String = "R15",
    accompaniedByCode: String = "L",
    accompaniedByComments: String? = "Information about the escort",
    comments: String? = "Some comments about the movement",
    location: Location = location(),
    legacyId: String = "${newId()}",
    recordedBy: String = DEFAULT_USERNAME,
    recordedAt: LocalDateTime = LocalDateTime.now().minusDays(7),
  ) = TapMovement(
    id,
    occurrenceId,
    occurredAt,
    direction,
    prisonCode,
    reasonCode,
    location,
    accompaniedByCode,
    accompaniedByComments,
    comments,
    AtAndBy(recordedAt, recordedBy),
    null,
    legacyId,
  )

  private fun syncTapMovement(
    personIdentifier: String,
    request: TapMovement,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC_TAP_MOVEMENT_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC_TAP_MOVEMENT_URL = "/sync/temporary-absence-movements/{personIdentifier}"
  }
}

private fun TemporaryAbsenceMovement.verifyAgainst(personIdentifier: String, request: TapMovement) {
  assertThat(this.person.identifier).isEqualTo(personIdentifier)
  assertThat(direction.name).isEqualTo(request.direction.name)
  assertThat(occurrence?.id).isEqualTo(request.occurrenceId)
  assertThat(occurredAt).isCloseTo(request.occurredAt, within(2, SECONDS))
  assertThat(absenceReason.code).isEqualTo(request.absenceReasonCode)
  assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
  assertThat(location).isEqualTo(request.location)
  assertThat(comments).isEqualTo(request.comments)
  assertThat(prisonCode).isEqualTo(request.prisonCode)
  assertThat(legacyId).isEqualTo(request.legacyId)
}
