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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.PersonSummaryOperations.Companion.verifyAgainst
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapAuthorisation
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SyncTapAuthorisationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(SYNC_TAP_AUTHORISATION_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    syncAuthorisation(personIdentifier(), tapAuthorisation(), "ROLE_ANY__OTHER_RW")
      .expectStatus().isForbidden
  }

  @Test
  fun `200 ok application created successfully including full path`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      prisonCode = prisonCode,
      statusCode = "PENDING",
      updated = null,
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
      ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "RDR",
      ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
      ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
    )
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application created successfully without a category`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      prisonCode = prisonCode,
      statusCode = "PENDING",
      subTypeCode = "SPL",
      reasonCode = "LTX",
      updated = null,
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
      ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "SPL",
      ReferenceDataDomain.Code.ABSENCE_REASON of "LTX",
    )
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application created successfully with type and subtype only`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      prisonCode = prisonCode,
      statusCode = "PENDING",
      subTypeCode = "CRL",
      reasonCode = "3",
      updated = null,
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SR",
      ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of "CRL",
    )
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application created for type only path`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      reasonCode = "PC",
      typeCode = "PP",
      subTypeCode = "PP",
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(ReferenceDataDomain.Code.ABSENCE_TYPE of "PP")
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationApproved(pi, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application created for security escort`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      reasonCode = "SE",
      typeCode = "SE",
      subTypeCode = null,
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SE",
      ReferenceDataDomain.Code.ABSENCE_REASON of "SE",
    )
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationApproved(pi, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application updated successfully`() {
    val legacyId = newId()
    val prisonCode = prisonCode()
    val ps = givenPersonSummary(personSummary())
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(ps.identifier))
    val existing = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        legacyId = legacyId,
        prisonCode = prisonCode,
        personIdentifier = ps.identifier,
      ),
    )
    val request = tapAuthorisation(id = existing.id, existing.prisonCode, legacyId = legacyId)
    val res = syncAuthorisation(existing.person.identifier, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(existing.id))
    saved.verifyAgainst(existing.person.identifier, request)
    val person = requireNotNull(findPersonSummary(ps.identifier))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationDateRangeChanged(saved.person.identifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok application created if authorisation with the given uuid does not exist`() {
    val pi = personIdentifier()
    val uuid = newUuid()
    val prisonCode = prisonCode()
    givenPersonSummary(personSummary(personIdentifier = pi))
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(id = uuid)
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(uuid)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationApproved(pi, saved.id, DataSource.NOMIS)))
  }

  private fun tapAuthorisation(
    id: UUID? = null,
    prisonCode: String = prisonCode(),
    reasonCode: String = "R15",
    typeCode: String? = "SR",
    subTypeCode: String? = "RDR",
    statusCode: String = "APPROVED",
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    repeat: Boolean = false,
    notes: String? = "Some notes about the application",
    fromDate: LocalDate = LocalDate.now().minusDays(7),
    toDate: LocalDate = LocalDate.now().minusDays(1),
    created: AtAndBy = AtAndBy(LocalDateTime.now().minusHours(1), DEFAULT_USERNAME),
    updated: AtAndBy? = AtAndBy(LocalDateTime.now().minusHours(1), SYSTEM_USERNAME),
    legacyId: Long = newId(),
  ) = TapAuthorisation(
    id,
    prisonCode,
    statusCode,
    typeCode,
    subTypeCode,
    reasonCode,
    accompaniedByCode,
    transportCode,
    repeat,
    fromDate,
    toDate,
    notes,
    created,
    updated,
    legacyId,
  )

  private fun syncAuthorisation(
    personIdentifier: String,
    request: TapAuthorisation,
    role: String? = Roles.NOMIS_SYNC,
  ) = webTestClient
    .put()
    .uri(SYNC_TAP_AUTHORISATION_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SYNC_TAP_AUTHORISATION_URL = "/sync/temporary-absence-authorisations/{personIdentifier}"
  }
}

private fun TemporaryAbsenceAuthorisation.verifyAgainst(personIdentifier: String, request: TapAuthorisation) {
  assertThat(person.identifier).isEqualTo(personIdentifier)
  assertThat(legacyId).isEqualTo(request.legacyId)
  assertThat(status.code).isEqualTo(request.statusCode)
  assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
  assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
  assertThat(absenceReason?.code).isEqualTo(request.absenceReasonCode)
  assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
  assertThat(prisonCode).isEqualTo(request.prisonCode)
  assertThat(repeat).isEqualTo(request.repeat)
  assertThat(notes).isEqualTo(request.notes)
  assertThat(fromDate).isEqualTo(request.fromDate)
  assertThat(toDate).isEqualTo(request.toDate)
}
