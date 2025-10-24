package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
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
    syncApplication(personIdentifier(), applicationRequest(), "ROLE_ANY__OTHER_RW")
      .expectStatus().isForbidden
  }

  @Test
  fun `200 ok application created successfully`() {
    val pi = personIdentifier()
    val request = applicationRequest(
      applicationStatus = "PEN",
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncApplication(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.approvedAt).isNull()
    assertThat(saved.approvedBy).isNull()

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!),
      setOf(),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
  }

  @Test
  fun `200 ok application updated successfully`() {
    val legacyId = newId()
    val existing = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(legacyId = legacyId))
    val request = applicationRequest(id = existing.id, existing.prisonCode, movementApplicationId = legacyId)
    val res = syncApplication(existing.personIdentifier, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(existing.id))
    saved.verifyAgainst(existing.personIdentifier, request)
    assertThat(saved.approvedAt).isNotNull
    assertThat(saved.approvedBy).isNotNull

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!),
      setOf(),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
  }

  @Test
  fun `200 ok application created if authorisation with the given uuid does not exist`() {
    val pi = personIdentifier()
    val uuid = newUuid()
    val request = applicationRequest(id = uuid)
    val res = syncApplication(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(uuid)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      setOf(TemporaryAbsenceAuthorised.EVENT_TYPE),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )
  }

  private fun applicationRequest(
    id: UUID? = null,
    prisonId: String? = prisonCode(),
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
