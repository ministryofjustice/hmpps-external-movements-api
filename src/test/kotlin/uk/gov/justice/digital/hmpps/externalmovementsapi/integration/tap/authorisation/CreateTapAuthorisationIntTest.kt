package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.postcode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.PersonSummaryOperations.Companion.verifyAgainst
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDate
import java.time.LocalDateTime

class CreateTapAuthorisationIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .post()
      .uri(CREATE_TAP_AUTH_URL, personIdentifier())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [TEMPORARY_ABSENCE_RO, EXTERNAL_MOVEMENTS_RO, EXTERNAL_MOVEMENTS_UI])
  fun `403 forbidden without correct role`(role: String) {
    createTapAuthorisation(
      personIdentifier(),
      createTapAuthorisationRequest(),
      role = role,
    ).expectStatus().isForbidden
  }

  @Test
  fun `404 prisoner does not exist`() {
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode(), setOf(pi), listOf())
    val request = createTapAuthorisationRequest()
    createTapAuthorisation(pi, request).expectStatus().isNotFound
  }

  @Test
  fun `400 location not valid`() {
    val pi = personIdentifier()
    val request = createTapAuthorisationRequest(
      occurrences = listOf(
        createTapOccurrenceRequest(
          location = location(description = null, address = null, postcode = null),
        ),
      ),
    )
    val res = createTapAuthorisation(pi, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Either a description or partial address must be specified.")
  }

  @Test
  fun `400 bad request - date range over 6 months`() {
    val pi = personIdentifier()
    val request = createTapAuthorisationRequest(start = LocalDate.now(), end = LocalDate.now().plusMonths(6).plusDays(1))
    val res = createTapAuthorisation(pi, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: The authorisation date range must not be more than 6 months")
  }

  @Test
  fun `200 ok tap authorisation created successfully`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapAuthorisationRequest()
    val username = word(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.absenceType?.code).isEqualTo(request.absenceTypeCode)
    assertThat(occurrence.absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
    assertThat(occurrence.absenceReasonCategory?.code).isEqualTo(request.absenceReasonCategoryCode)
    assertThat(occurrence.absenceReason.code).isEqualTo(request.absenceReasonCode)
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username),
    )

    verifyEvents(saved, setOf(TemporaryAbsenceAuthorisationPending(pi, saved.id)))
  }

  @Test
  fun `200 ok tap authorisation created successfully using default links`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request =
      createTapAuthorisationRequest(
        absenceTypeCode = "PP",
        absenceSubTypeCode = null,
        absenceReasonCategoryCode = null,
        absenceReasonCode = null,
        statusCode = AuthorisationStatus.Code.APPROVED,
      )
    val username = word(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request.copy(absenceSubTypeCode = "PP", absenceReasonCode = "PC"))

    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.absenceType?.code).isEqualTo("PP")
    assertThat(occurrence.absenceSubType?.code).isEqualTo("PP")
    assertThat(occurrence.absenceReasonCategory?.code).isNull()
    assertThat(occurrence.absenceReason.code).isEqualTo("PC")
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username),
    )

    verifyEvents(
      saved,
      listOf(TemporaryAbsenceScheduled(pi, occurrence.id), TemporaryAbsenceAuthorisationApproved(pi, saved.id)).toSet(),
    )
  }

  @Test
  fun `200 ok tap authorisation created successfully using category`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request =
      createTapAuthorisationRequest(
        absenceTypeCode = "SR",
        absenceSubTypeCode = "RDR",
        absenceReasonCategoryCode = "ET",
        absenceReasonCode = null,
        statusCode = AuthorisationStatus.Code.APPROVED,
      )
    val username = word(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request.copy(absenceReasonCode = "ET"))

    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.absenceType?.code).isEqualTo("SR")
    assertThat(occurrence.absenceSubType?.code).isEqualTo("RDR")
    assertThat(occurrence.absenceReasonCategory?.code).isEqualTo("ET")
    assertThat(occurrence.absenceReason.code).isEqualTo("ET")
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username),
    )

    verifyEvents(
      saved,
      listOf(TemporaryAbsenceScheduled(pi, occurrence.id), TemporaryAbsenceAuthorisationApproved(pi, saved.id)).toSet(),
    )
  }

  @Test
  fun `200 ok tap authorisation created successfully type and reason`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request =
      createTapAuthorisationRequest(
        absenceTypeCode = "SE",
        absenceSubTypeCode = null,
        absenceReasonCategoryCode = null,
        absenceReasonCode = "C4",
        statusCode = AuthorisationStatus.Code.APPROVED,
      )
    val username = word(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)

    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.absenceType?.code).isEqualTo("SE")
    assertThat(occurrence.absenceSubType?.code).isNull()
    assertThat(occurrence.absenceReasonCategory?.code).isNull()
    assertThat(occurrence.absenceReason.code).isEqualTo("C4")
    assertThat(occurrence.reasonPath.path).containsExactly(ABSENCE_TYPE of "SE", ABSENCE_REASON of "C4")
    val person = requireNotNull(findPersonSummary(pi))
    person.verifyAgainst(prisoners.first())

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
        HmppsDomainEvent::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username),
    )

    verifyEvents(
      saved,
      listOf(TemporaryAbsenceScheduled(pi, occurrence.id), TemporaryAbsenceAuthorisationApproved(pi, saved.id)).toSet(),
    )
  }

  private fun createTapOccurrenceRequest(
    start: LocalDateTime = LocalDateTime.now().minusHours(3),
    end: LocalDateTime = LocalDateTime.now().plusHours(3),
    location: Location = location(),
    scheduleReference: JsonNode? = null,
  ) = CreateTapAuthorisationRequest.OccurrenceRequest(
    start = start,
    end = end,
    location = location,
    scheduleReference = scheduleReference,
  )

  private fun location(
    description: String? = word(10),
    address: String? = "${word(8)} ${word(4)} ${word(8)}",
    postcode: String? = postcode(),
    uprn: Long? = newUuid().mostSignificantBits,
  ): Location = Location(description, address, postcode, uprn)

  private fun createTapAuthorisationRequest(
    absenceTypeCode: String = "SR",
    absenceSubTypeCode: String? = "RDR",
    absenceReasonCategoryCode: String? = "PW",
    absenceReasonCode: String? = "R15",
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    start: LocalDate = LocalDate.now().minusDays(3),
    end: LocalDate = LocalDate.now().plusDays(1),
    statusCode: AuthorisationStatus.Code = AuthorisationStatus.Code.PENDING,
    occurrences: List<CreateTapAuthorisationRequest.OccurrenceRequest> = listOf(createTapOccurrenceRequest()),
    comments: String? = null,
    repeat: Boolean = false,
    contactInformation: String? = null,
  ) = CreateTapAuthorisationRequest(
    absenceTypeCode,
    absenceSubTypeCode,
    absenceReasonCategoryCode,
    absenceReasonCode,
    occurrences,
    statusCode,
    accompaniedByCode,
    transportCode,
    comments,
    repeat,
    start,
    end,
    contactInformation,
  )

  private fun createTapAuthorisation(
    personIdentifier: String,
    request: CreateTapAuthorisationRequest,
    username: String = DEFAULT_USERNAME,
    role: String? = Roles.TEMPORARY_ABSENCE_RW,
  ) = webTestClient
    .post()
    .uri(CREATE_TAP_AUTH_URL, personIdentifier)
    .bodyValue(request)
    .headers(setAuthorisation(username = username, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val CREATE_TAP_AUTH_URL = "/temporary-absence-authorisations/{personIdentifier}"
  }
}
