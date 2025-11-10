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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.postcode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.NomisAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovementRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapMovementRequest.Direction
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class SyncTemporaryAbsenceMovementIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
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
      tapMovementRequest(direction = Direction.OUT),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request if person identifier does not match occurrence person identifier`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))
    val request = tapMovementRequest(
      direction = Direction.IN,
      occurrenceId = occurrence.id,
      prisonCode = authorisation.prisonCode,
    )
    syncTapMovement(personIdentifier(), request).expectStatus().isBadRequest
  }

  @Test
  fun `200 ok temporary absence created successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation())
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))
    val request = tapMovementRequest(
      direction = Direction.OUT,
      occurrenceId = occurrence.id,
      prisonCode = authorisation.prisonCode,
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncTapMovement(authorisation.personIdentifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceMovement(res.id))
    saved.verifyAgainst(authorisation.personIdentifier, request)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!, TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )
    verifyAudit(
      occurrence,
      RevisionType.MOD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!, TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )
    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok temporary absence updated successfully`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val existing = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        TemporaryAbsenceMovement.Direction.OUT,
        authorisation.personIdentifier,
        occurrence,
        legacyId = newId().toString(),
      ),
    )
    val request = tapMovementRequest(
      escortCode = "U",
      escortText = "Updated the text about the escort",
      direction = Direction.OUT,
      id = existing.id,
      occurrenceId = occurrence.id,
      legacyId = existing.legacyId!!,
    )
    val res = syncTapMovement(authorisation.personIdentifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceMovement(existing.id))
    saved.verifyAgainst(authorisation.personIdentifier, request)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok temporary absence id returned if legacy id already exists`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = newId()))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation, legacyId = newId()))
    val existing = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(
        TemporaryAbsenceMovement.Direction.IN,
        authorisation.personIdentifier,
        occurrence,
        legacyId = newId().toString(),
      ),
    )
    val request = tapMovementRequest(
      escortCode = "U",
      escortText = "Updated the text about the escort",
      direction = Direction.IN,
      occurrenceId = occurrence.id,
      legacyId = existing.legacyId!!,
    )
    val res = syncTapMovement(authorisation.personIdentifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceMovement(existing.id))
    saved.verifyAgainst(authorisation.personIdentifier, request)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceMovement::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok absence created if one doesn't exist with the given uuid`() {
    val personIdentifier = personIdentifier()
    val request = tapMovementRequest(id = newUuid(), direction = Direction.IN)
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
      setOf(TemporaryAbsenceMovement::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )
    verifyEvents(saved, setOf())
  }

  private fun tapMovementRequest(
    id: UUID? = null,
    occurrenceId: UUID? = null,
    direction: Direction,
    prisonCode: String = prisonCode(),
    movementDateTime: LocalDateTime = LocalDateTime.now().minusDays(7),
    movementReason: String = "R15",
    escortCode: String? = "L",
    escortText: String? = "Information about the escort",
    comment: String? = "Some notes about the movement",
    location: TapLocation = TapLocation(description = name(10), postcode = postcode()),
    legacyId: String = "${newId()}",
    audit: NomisAudit = NomisAuditGenerator.generate(),
  ) = TapMovementRequest(
    id,
    occurrenceId,
    legacyId,
    movementDateTime,
    movementReason,
    direction,
    escortCode,
    escortText,
    prisonCode,
    comment,
    location,
    audit,
  )

  private fun syncTapMovement(
    personIdentifier: String,
    request: TapMovementRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC_TAP_MOVEMENT_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC_TAP_MOVEMENT_URL = "/sync/temporary-absence-movement/{personIdentifier}"
  }
}

private fun TemporaryAbsenceMovement.verifyAgainst(personIdentifier: String, request: TapMovementRequest) {
  assertThat(this.personIdentifier).isEqualTo(personIdentifier)
  assertThat(direction.name).isEqualTo(request.direction.name)
  assertThat(occurrence?.id).isEqualTo(request.occurrenceId)
  assertThat(occurredAt).isCloseTo(request.movementDateTime, within(2, SECONDS))
  assertThat(absenceReason.code).isEqualTo(request.movementReason)
  assertThat(accompaniedBy.code).isEqualTo(request.escortOrDefault())
  assertThat(recordedByPrisonCode).isEqualTo(request.prisonCodeOrDefault())
  assertThat(location).isEqualTo(request.location.asLocation())
  assertThat(location.description).isEqualTo(request.location.description)
  assertThat(notes).isEqualTo(request.commentText)
  assertThat(recordedBy).isEqualTo(request.audit.createUsername)
  assertThat(recordedAt).isCloseTo(request.audit.createDatetime, within(1, SECONDS))
  assertThat(legacyId).isEqualTo(request.legacyId)
}
