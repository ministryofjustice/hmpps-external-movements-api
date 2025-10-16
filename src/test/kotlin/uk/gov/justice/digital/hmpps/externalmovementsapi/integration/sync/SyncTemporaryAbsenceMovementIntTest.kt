package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.description
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.NomisAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapMovementRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapMovementRequest.Direction
import java.time.LocalDateTime
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
    location: TapLocation = TapLocation(id = newId().toString(), description = name(10)),
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
