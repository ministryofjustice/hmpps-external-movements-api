package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.NomisAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapApplicationRequest
import java.time.LocalDate
import java.util.UUID

class SyncTapApplicationIntTest(
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYN_TAP_APPLICATION_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    syncApplication(
      personIdentifier(),
      applicationRequest(prisonId = "NE1"),
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `200 ok application created successfully`() {
    val pi = personIdentifier()
    val request = applicationRequest(
      prisonId = "TAC",
      applicationStatus = "PEN",
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncApplication(pi, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.approvedAt).isNull()
    assertThat(saved.approvedBy).isNull()
  }

  @Test
  fun `200 ok application updated successfully`() {
    val prisonCode = "TAU"
    val legacyId = newId()
    val existing = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, legacyId = legacyId))
    val request = applicationRequest(id = existing.id, prisonId = prisonCode, movementApplicationId = legacyId)
    val res = syncApplication(existing.personIdentifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(existing.id))
    saved.verifyAgainst(existing.personIdentifier, request)
    assertThat(saved.approvedAt).isNotNull
    assertThat(saved.approvedBy).isNotNull
  }

  @Test
  fun `200 ok application created if authorisation with the given uuid does not exist`() {
    val pi = personIdentifier()
    val uuid = newUuid()
    val request = applicationRequest(id = uuid, prisonId = "TAZ")
    val res = syncApplication(pi, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(uuid)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
  }

  private fun applicationRequest(
    id: UUID? = null,
    prisonId: String?,
    eventSubType: String = "R15",
    temporaryAbsenceType: String? = "SR",
    temporaryAbsenceSubType: String? = "RDR",
    applicationStatus: String = "APP-SCH",
    applicationDate: LocalDate = LocalDate.now().minusMonths(1),
    comment: String? = "Some notes about the application",
    movementApplicationId: Long = newId(),
    contactPersonName: String? = null,
    applicationType: String = "SINGLE",
    fromDate: LocalDate = LocalDate.now().minusDays(7),
    toDate: LocalDate = LocalDate.now().minusDays(1),
    audit: NomisAudit = NomisAuditGenerator.generate(),
  ) = TapApplicationRequest(
    id,
    movementApplicationId,
    eventSubType,
    applicationDate,
    applicationStatus,
    comment,
    prisonId,
    contactPersonName,
    applicationType,
    temporaryAbsenceType,
    temporaryAbsenceSubType,
    fromDate,
    toDate,
    audit,
  )

  private fun syncApplication(
    personIdentifier: String,
    request: TapApplicationRequest,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(SYN_TAP_APPLICATION_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYN_TAP_APPLICATION_URL = "/sync/temporary-absence-application/{personIdentifier}"
  }
}
