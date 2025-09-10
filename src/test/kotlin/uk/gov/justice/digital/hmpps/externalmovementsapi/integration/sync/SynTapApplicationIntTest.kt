package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceSeriesOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceSeriesOperations.Companion.temporaryAbsenceSeries
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TestConfig
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.NomisAudit
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.TapApplicationRequest
import java.time.LocalDate
import java.time.LocalDateTime

@ContextConfiguration(classes = [TestConfig::class])
class SynTapApplicationIntTest(
  @Autowired private val tasOperations: TempAbsenceSeriesOperations,
) : IntegrationTest(),
  TempAbsenceSeriesOperations by tasOperations {

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
    syncApplication(personIdentifier(), applicationRequest("NE1"), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `200 ok application created successfully`() {
    val pi = personIdentifier()
    val request = applicationRequest("TAC")
    val res = syncApplication(pi, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceSeries(res.id))
    saved.verifyAgainst(pi, request)
  }

  @Test
  fun `200 ok application updated successfully`() {
    val prisonCode = "TAU"
    val legacyId = newId()
    val existing = givenTemporaryAbsenceSeries(temporaryAbsenceSeries(prisonCode, legacyId = legacyId))
    val request = applicationRequest(prisonCode, movementApplicationId = legacyId)
    val res = syncApplication(existing.personIdentifier, request)
      .expectStatus().isOk
      .expectBody<SyncResponse>()
      .returnResult()
      .responseBody!!

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceSeries(existing.id))
    saved.verifyAgainst(existing.personIdentifier, request)
  }

  private fun applicationRequest(
    prisonId: String?,
    eventSubType: String = "R15",
    temporaryAbsenceType: String? = "SR",
    temporaryAbsenceSubType: String? = "RDR",
    applicationStatus: String = "APP-SCH",
    applicationDate: LocalDate = LocalDate.now().minusMonths(1),
    fromDate: LocalDate = LocalDate.now().minusDays(7),
    releaseTime: LocalDateTime = LocalDateTime.now().minusDays(7),
    toDate: LocalDate = LocalDate.now(),
    returnTime: LocalDateTime = LocalDateTime.now(),
    escortCode: String? = "L",
    transportType: String? = "OD",
    comment: String? = "Some notes about the application",
    movementApplicationId: Long = newId(),
    toAgencyId: String? = null,
    toAddressId: Long? = null,
    toAddressOwnerClass: String? = null,
    contactPersonName: String? = null,
    applicationType: String = "SINGLE",
    audit: NomisAudit = NomisAuditGenerator.generate(),
  ) = TapApplicationRequest(
    movementApplicationId,
    eventSubType,
    applicationDate,
    fromDate,
    releaseTime,
    toDate,
    returnTime,
    applicationStatus,
    escortCode,
    transportType,
    comment,
    prisonId,
    toAgencyId,
    toAddressId,
    toAddressOwnerClass,
    contactPersonName,
    applicationType,
    temporaryAbsenceType,
    temporaryAbsenceSubType,
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
