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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationAccompanimentChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationCommentsChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDateRangeChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationDeferred
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationTransportChanged
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.PersonSummaryOperations.Companion.verifyAgainst
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.AuthorisationSchedule
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapAuthorisation
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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
  fun `200 ok authorisation created successfully including full path`() {
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
    assertThat(saved.locations).containsExactly(request.location)
    assertThat(saved.schedule).isNotNull
    val schedule: AuthorisationSchedule = jsonMapper.treeToValue(saved.schedule!!)
    assertThat(schedule.startTime).isEqualTo(request.startTime)
    assertThat(schedule.returnTime).isEqualTo(request.endTime)
    assertThat(schedule.type).isEqualTo("SINGLE")
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)),
    )
  }

  @Test
  fun `200 ok authorisation created where category is required but type and sub type missing`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      prisonCode = prisonCode,
      statusCode = "PENDING",
      end = now().minusDays(1),
      typeCode = null,
      subTypeCode = null,
      updated = null,
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.EXPIRED.name)
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of "PW",
      ReferenceDataDomain.Code.ABSENCE_REASON of "R15",
    )
    assertThat(saved.locations).containsExactly(request.location)
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)),
    )
  }

  @Test
  fun `200 ok authorisation created successfully without a category`() {
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
    assertThat(saved.locations).containsExactly(request.location)
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)),
    )
  }

  @Test
  fun `200 ok authorisation created successfully with type and subtype only`() {
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
    assertThat(saved.locations).containsExactly(request.location)
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(TemporaryAbsenceAuthorisationPending(saved.person.identifier, saved.id, DataSource.NOMIS)),
    )
  }

  @Test
  fun `200 ok authorisation created for type only path`() {
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
    assertThat(saved.locations).containsExactly(request.location)
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
  fun `200 ok authorisation created for security escort`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = tapAuthorisation(
      reasonCode = "SE",
      typeCode = "SE",
      subTypeCode = "SE",
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.absenceSubType).isNull()
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SE",
      ReferenceDataDomain.Code.ABSENCE_REASON of "SE",
    )
    assertThat(saved.locations).containsExactly(request.location)
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
  fun `200 ok authorisation updated for security escort`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, pi, locations = linkedSetOf(location()), legacyId = newId()))
    val request = tapAuthorisation(
      id = auth.id,
      reasonCode = "SE",
      typeCode = "SE",
      subTypeCode = "SE",
      accompaniedByCode = auth.accompaniedBy.code,
      transportCode = auth.transport.code,
      start = auth.start,
      end = auth.end,
      location = auth.locations.single(),
      comments = auth.comments,
      legacyId = auth.legacyId!!,
    )
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.absenceSubType).isNull()
    assertThat(saved.reasonPath.path).containsExactly(
      ReferenceDataDomain.Code.ABSENCE_TYPE of "SE",
      ReferenceDataDomain.Code.ABSENCE_REASON of "SE",
    )
    assertThat(saved.locations).containsExactly(request.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationRecategorised(pi, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok authorisation updated successfully`() {
    val legacyId = newId()
    val prisonCode = prisonCode()
    val ps = givenPersonSummary(personSummary())
    val existing = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        legacyId = legacyId,
        prisonCode = prisonCode,
        personIdentifier = ps.identifier,
        accompaniedByCode = "U",
        transportCode = "TNR",
        locations = linkedSetOf(location()),
      ),
    )
    val request = tapAuthorisation(id = existing.id, existing.prisonCode, location = existing.locations.single(), legacyId = legacyId)
    val res = syncAuthorisation(existing.person.identifier, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(existing.id))
    saved.verifyAgainst(existing.person.identifier, request)
    assertThat(saved.locations).containsExactly(request.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(
      saved,
      setOf(
        TemporaryAbsenceAuthorisationAccompanimentChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceAuthorisationDateRangeChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceAuthorisationCommentsChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
        TemporaryAbsenceAuthorisationTransportChanged(saved.person.identifier, saved.id, DataSource.NOMIS),
      ),
    )
  }

  @Test
  fun `200 ok approved authorisation returned to pending`() {
    val legacyId = newId()
    val prisonCode = prisonCode()
    val ps = givenPersonSummary(personSummary())
    val existing = givenTemporaryAbsenceAuthorisation(
      temporaryAbsenceAuthorisation(
        legacyId = legacyId,
        prisonCode = prisonCode,
        personIdentifier = ps.identifier,
        locations = linkedSetOf(location()),
      ),
    )
    val request = tapAuthorisation(
      id = existing.id,
      prisonCode = existing.prisonCode,
      legacyId = legacyId,
      statusCode = "PENDING",
      comments = existing.comments,
      start = existing.start,
      end = existing.end,
      location = existing.locations.single(),
    )
    val res = syncAuthorisation(existing.person.identifier, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(existing.id)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(existing.id))
    saved.verifyAgainst(existing.person.identifier, request)
    assertThat(saved.status.code).isEqualTo(AuthorisationStatus.Code.PENDING.name)
    assertThat(saved.locations).containsExactly(request.location)

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationDeferred(saved.person.identifier, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok authorisation created if authorisation with the given uuid does not exist`() {
    val pi = personIdentifier()
    val uuid = newUuid()
    givenPersonSummary(personSummary(personIdentifier = pi))
    val request = tapAuthorisation(id = uuid)
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    assertThat(res.id).isEqualTo(uuid)
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.locations).containsExactly(request.location)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME, source = DataSource.NOMIS),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationApproved(pi, saved.id, DataSource.NOMIS)))
  }

  @Test
  fun `200 ok - can create authorisation with date range over 6 months`() {
    val pi = personIdentifier()
    givenPersonSummary(personSummary(personIdentifier = pi))
    val request = tapAuthorisation(start = now(), end = now().plusMonths(6).plusDays(1))
    val res = syncAuthorisation(pi, request).successResponse<SyncResponse>()

    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.locations).containsExactly(request.location)

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
    comments: String? = "Some comments about the application",
    start: LocalDate = now().minusDays(7),
    end: LocalDate = now(),
    startTime: LocalTime = LocalTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MINUTES),
    endTime: LocalTime = LocalDateTime.of(start, startTime).plusHours(2).toLocalTime().truncatedTo(ChronoUnit.MINUTES),
    location: Location = location(),
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
    start,
    end,
    startTime,
    endTime,
    location,
    comments,
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
  if (request.statusCode == AuthorisationStatus.Code.PENDING.name && request.end.isBefore(now())) {
    assertThat(status.code).isEqualTo(AuthorisationStatus.Code.EXPIRED.name)
  } else {
    assertThat(status.code).isEqualTo(request.statusCode)
  }
  assertThat(absenceType?.code).isEqualTo(request.absenceTypeCode)
  assertThat(absenceSubType?.code).isEqualTo(request.absenceSubTypeCode.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value })
  assertThat(absenceReason.code).isEqualTo(request.absenceReasonCode)
  assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
  assertThat(prisonCode).isEqualTo(request.prisonCode)
  assertThat(repeat).isEqualTo(request.repeat)
  assertThat(comments).isEqualTo(request.comments)
  assertThat(start).isEqualTo(request.start)
  assertThat(end).isEqualTo(request.end)
  assertThat(accompaniedBy.code).isEqualTo(request.accompaniedByCode)
  assertThat(transport.code).isEqualTo(request.transportCode)
}
