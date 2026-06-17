package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.CaseloadIdHeader
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRelocated
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.username
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.CreateOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.AuthorisationActions
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.CreateOccurrences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class CreateTapOccurrenceIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(CREATE_OCCURRENCE_URL, IdGenerator.newUuid())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW, Roles.EXTERNAL_MOVEMENTS_RO, Roles.EXTERNAL_MOVEMENTS_RW])
  fun `403 forbidden without correct role`(role: String) {
    createOccurrence(
      UUID.randomUUID(),
      createOccurrenceRequest(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 authorisation does not exist`() {
    createOccurrence(IdGenerator.newUuid(), createOccurrenceRequest()).expectStatus().isNotFound
  }

  @ParameterizedTest
  @MethodSource("validationRequests")
  fun `400 bad request - validation exceptions`(request: CreateOccurrenceRequest, message: String) {
    val authorisation =
      givenTemporaryAbsenceAuthorisation(TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation())
    val response = createOccurrence(authorisation.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(response.developerMessage).startsWith(message)
  }

  @Test
  fun `400 bad request - cannot add multiple occurrences to a single authorisation`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        start = LocalDate.now(),
        end = LocalDate.now().plusDays(3),
      ),
    )
    givenTemporaryAbsenceOccurrence(
      TempAbsenceOccurrenceOperations.temporaryAbsenceOccurrence(
        auth,
        start = LocalDateTime.now().plusDays(1),
        end = LocalDateTime.now().plusDays(2),
      ),
    )
    val res = createOccurrence(auth.id, createOccurrenceRequest()).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Invalid request")
  }

  @Test
  fun `400 bad request - cannot add multiple occurrences to a single authorisation sending multiples`() {
    val auth = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        start = LocalDate.now(),
        end = LocalDate.now().plusDays(3),
      ),
    )
    val res = createOccurrences(auth.id, listOf(createOccurrenceRequest(), createOccurrenceRequest()))
      .errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Invalid request")
  }

  @Test
  fun `200 ok - an occurrence is added to an authorisation with comments`() {
    val authorisation =
      givenTemporaryAbsenceAuthorisation(TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation())
    val request = createOccurrenceRequest()
    val reason = word(12)
    val username = username()
    val caseloadId = word(6)
    val res = createOccurrence(authorisation.id, request, reason, username, caseloadId)
      .successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationRelocated.EVENT_TYPE)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change(
        "locations",
        emptyList<String>(),
        listOf(request.location).map { it.toString() },
      ),
    )
    assertThat(res.reason).isEqualTo(reason)

    val occurrence = findForAuthorisation(authorisation.id).first()
    occurrence.verifyAgainst(request)
    occurrence.verifyAgainst(authorisation)

    verifyAudit(
      occurrence,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username, caseloadId = caseloadId, reason = reason),
    )

    verifyEventPublications(
      occurrence,
      setOf(
        TemporaryAbsenceScheduled.Companion(occurrence.person.identifier, occurrence.id).publication(occurrence.id),
        TemporaryAbsenceAuthorisationRelocated.Companion(authorisation.person.identifier, authorisation.id).publication(
          authorisation.id,
        ),
      ),
    )
  }

  @Test
  fun `200 ok - an occurrence is added to an authorisation without comments`() {
    val authorisation =
      givenTemporaryAbsenceAuthorisation(TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation())
    val request = createOccurrenceRequest(comments = null)
    val username = username()
    val caseloadId = word(6)
    createOccurrence(authorisation.id, request, username = username, caseloadId = caseloadId).expectStatus().isOk

    val occurrence = findForAuthorisation(authorisation.id).first()
    assertThat(occurrence.authorisation.id).isEqualTo(authorisation.id)
    occurrence.verifyAgainst(request)
    occurrence.verifyAgainst(authorisation)

    verifyAudit(
      occurrence,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username, caseloadId = caseloadId),
    )

    verifyEventPublications(
      occurrence,
      setOf(
        TemporaryAbsenceScheduled.Companion(occurrence.person.identifier, occurrence.id).publication(occurrence.id),
        TemporaryAbsenceAuthorisationRelocated.Companion(authorisation.person.identifier, authorisation.id).publication(
          authorisation.id,
        ),
      ),
    )
  }

  @Test
  fun `200 ok - an occurrence is added to an authorisation with new location`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        repeat = true,
        locations = linkedSetOf(LocationGenerator.location()),
      ),
    )
    val occ1 = givenTemporaryAbsenceOccurrence(
      TempAbsenceOccurrenceOperations.temporaryAbsenceOccurrence(
        authorisation,
        location = authorisation.locations.single(),
      ),
    )

    val request = createOccurrenceRequest()
    val reason = word(20)
    val username = username()
    val caseloadId = word(6)
    val res = createOccurrence(authorisation.id, request, reason, username, caseloadId)
      .successResponse<AuditHistory>().content.single()
    assertThat(res.domainEvents).containsExactly(TemporaryAbsenceAuthorisationRelocated.EVENT_TYPE)
    assertThat(res.changes).containsExactly(
      AuditedAction.Change(
        "locations",
        authorisation.locations.map { it.toString() },
        (authorisation.locations + request.location).map { it.toString() },
      ),
    )
    assertThat(res.reason).isEqualTo(reason)

    val occurrence = findForAuthorisation(authorisation.id).first { it.id != occ1.id }
    occurrence.verifyAgainst(request)
    occurrence.verifyAgainst(authorisation)
    assertThat(occurrence.authorisation.locations).containsExactly(occ1.location, occurrence.location)

    verifyAudit(
      occurrence,
      RevisionType.ADD,
      setOf(
        HmppsDomainEvent::class.simpleName!!,
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username, caseloadId = caseloadId, reason = reason),
    )

    verifyEventPublications(
      authorisation,
      setOf(
        TemporaryAbsenceScheduled.Companion(occurrence.person.identifier, occurrence.id).publication(occurrence.id),
        TemporaryAbsenceAuthorisationRelocated.Companion(authorisation.person.identifier, authorisation.id).publication(
          authorisation.id,
        ),
      ),
    )
  }

  @Test
  fun `200 ok - an occurrence is added to a paused authorisation`() {
    val authorisation = givenTemporaryAbsenceAuthorisation(
      TempAbsenceAuthorisationOperations.temporaryAbsenceAuthorisation(
        repeat = true,
        status = AuthorisationStatus.Code.PAUSED,
        locations = linkedSetOf(LocationGenerator.location()),
      ),
    )
    assertThat(authorisation.status.code).isEqualTo(AuthorisationStatus.Code.PAUSED.name)
    val occ1 = givenTemporaryAbsenceOccurrence(
      TempAbsenceOccurrenceOperations.temporaryAbsenceOccurrence(
        authorisation,
        location = authorisation.locations.single(),
        dpsOnly = true,
      ),
    )
    assertThat(occ1.status.code).isEqualTo(OccurrenceStatus.Code.PAUSED.name)

    val request = createOccurrenceRequest(location = occ1.location)
    createOccurrence(authorisation.id, request).expectStatus().isOk

    val occurrence = findForAuthorisation(authorisation.id).first { it.id != occ1.id }
    occurrence.verifyAgainst(request)
    assertThat(occurrence.status.code).isEqualTo(OccurrenceStatus.Code.PAUSED.name)

    verifyAudit(
      occurrence,
      RevisionType.ADD,
      setOf(TemporaryAbsenceOccurrence::class.simpleName!!),
      ExternalMovementContext.get().copy(username = DEFAULT_USERNAME),
    )

    verifyEvents(occurrence, setOf())
  }

  private fun TemporaryAbsenceOccurrence.verifyAgainst(request: CreateOccurrenceRequest) {
    assertThat(start).isCloseTo(request.start, Assertions.within(1, ChronoUnit.SECONDS))
    assertThat(end).isCloseTo(request.end, Assertions.within(1, ChronoUnit.SECONDS))
    assertThat(location).isEqualTo(request.location)
    assertThat(comments).isEqualTo(request.comments ?: authorisation.comments)
  }

  private fun TemporaryAbsenceOccurrence.verifyAgainst(authorisation: TemporaryAbsenceAuthorisation) {
    assertThat(absenceType?.code).isEqualTo(authorisation.absenceType?.code)
    assertThat(absenceSubType?.code).isEqualTo(authorisation.absenceSubType?.code)
    assertThat(absenceReasonCategory?.code).isEqualTo(authorisation.absenceReasonCategory?.code)
    assertThat(absenceReason.code).isEqualTo(authorisation.absenceReason.code)
    assertThat(accompaniedBy.code).isEqualTo(authorisation.accompaniedBy.code)
    assertThat(transport.code).isEqualTo(authorisation.transport.code)
  }

  private fun createOccurrence(
    id: UUID,
    request: CreateOccurrenceRequest,
    reason: String? = null,
    username: String = DEFAULT_USERNAME,
    caseloadId: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = createOccurrences(id, listOf(request), reason, username, caseloadId, role)

  private fun createOccurrences(
    id: UUID,
    requests: List<CreateOccurrenceRequest>,
    reason: String? = null,
    username: String = DEFAULT_USERNAME,
    caseloadId: String? = null,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .put()
    .uri(CREATE_OCCURRENCE_URL, id)
    .bodyValue(AuthorisationActions(listOf(CreateOccurrences(requests)), reason))
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .headers { h -> caseloadId?.also { h.put(CaseloadIdHeader.NAME, listOf(it)) } }
    .exchange()

  companion object {
    const val CREATE_OCCURRENCE_URL = "/temporary-absence-authorisations/{id}"

    private fun createOccurrenceRequest(
      start: LocalDateTime = LocalDateTime.now().plusDays(1),
      end: LocalDateTime = LocalDateTime.now().plusDays(2),
      location: Location = LocationGenerator.location(),
      comments: String? = word(20),
    ) = CreateOccurrenceRequest(start, end, location, comments)

    @JvmStatic
    fun validationRequests() = listOf(
      Arguments.of(
        createOccurrenceRequest(start = LocalDateTime.now().minusDays(5), end = LocalDateTime.now().minusDays(4)),
        "Validation failure: Absence cannot be scheduled in the past.",
      ),
      Arguments.of(
        createOccurrenceRequest(start = LocalDateTime.now().plusDays(5), end = LocalDateTime.now().plusDays(4)),
        "Validation failure: End must be after start.",
      ),
      Arguments.of(
        createOccurrenceRequest(
          location = LocationGenerator.location(
            description = null,
            address = null,
            postcode = null,
          ),
        ),
        "Validation failure: Either a description or partial address must be specified.",
      ),
      Arguments.of(
        createOccurrenceRequest(start = LocalDateTime.now().plusDays(7), end = LocalDateTime.now().plusDays(8)),
        "IllegalStateException:",
      ),
    )
  }
}
