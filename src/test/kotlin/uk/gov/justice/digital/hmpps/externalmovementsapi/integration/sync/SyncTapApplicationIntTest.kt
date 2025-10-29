package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
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
import java.time.temporal.ChronoUnit.SECONDS
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
  fun `200 ok application created successfully including full path`() {
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
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
      ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "RDR",
      ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
      ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
    )

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok application created successfully without a category`() {
    val pi = personIdentifier()
    val request = applicationRequest(
      applicationStatus = "PEN",
      temporaryAbsenceSubType = "SPL",
      eventSubType = "LTX",
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncApplication(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.approvedAt).isNull()
    assertThat(saved.approvedBy).isNull()
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
      ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "SPL",
      ReferenceDataDomain.Code.ABSENCE_REASON of "LTX",
    )

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok application created successfully with type and subtype only`() {
    val pi = personIdentifier()
    val request = applicationRequest(
      applicationStatus = "PEN",
      temporaryAbsenceSubType = "CRL",
      eventSubType = "3",
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncApplication(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.approvedAt).isNull()
    assertThat(saved.approvedBy).isNull()
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
      ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "CRL",
    )

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok application created for type only path`() {
    val pi = personIdentifier()
    val request = applicationRequest(
      eventSubType = "PC",
      temporaryAbsenceType = "PP",
      temporaryAbsenceSubType = "PP",
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncApplication(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorised(pi, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application created for security escort`() {
    val pi = personIdentifier()
    val request = applicationRequest(
      eventSubType = "SE",
      temporaryAbsenceType = "SE",
      temporaryAbsenceSubType = null,
      audit = NomisAuditGenerator.generate(modifiedAt = null, modifiedBy = null),
    )
    val res = syncApplication(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SE",
      ReferenceDataDomain.Code.ABSENCE_REASON of "SE",
    )

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorised(pi, saved.id, DataSource.NOMIS)))
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
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf())
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
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorised(pi, saved.id, DataSource.NOMIS)))
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

private fun TemporaryAbsenceAuthorisation.verifyAgainst(personIdentifier: String, request: TapApplicationRequest) {
  assertThat(this.personIdentifier).isEqualTo(personIdentifier)
  assertThat(legacyId).isEqualTo(request.movementApplicationId)
  assertThat(status.code).isEqualTo(request.tapAuthStatusCode.name)
  assertThat(absenceType?.code).isEqualTo(request.temporaryAbsenceType)
  assertThat(absenceSubType?.code).isEqualTo(request.temporaryAbsenceSubType)
  assertThat(absenceReason?.code).isEqualTo(request.eventSubType)
  assertThat(prisonCode).isEqualTo(request.prisonId)
  assertThat(repeat).isEqualTo(request.isRepeating())
  assertThat(notes).isEqualTo(request.comment)
  assertThat(fromDate).isEqualTo(request.fromDate)
  assertThat(toDate).isEqualTo(request.toDate)
  assertThat(applicationDate).isEqualTo(request.applicationDate)
  assertThat(submittedAt).isCloseTo(request.audit.createDatetime, within(1, SECONDS))
  assertThat(submittedBy).isEqualTo(request.audit.createUsername)
  approvedAt?.also {
    assertThat(it).isCloseTo(request.approvedAt, within(1, SECONDS))
  }
  assertThat(approvedBy).isEqualTo(request.approvedBy)
}
