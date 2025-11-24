package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationPending
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceScheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.PersonSummaryOperations.Companion.verifyAgainst
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapOccurrenceRequest
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

  @Test
  fun `403 forbidden without correct role`() {
    createTapAuthorisation(
      personIdentifier(),
      createTapAuthorisationRequest(),
      role = "ROLE_ANY__OTHER_RW",
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
          location = location(description = null, address = null),
        ),
      ),
    )
    val res = createTapAuthorisation(pi, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(res.userMessage).isEqualTo("Validation failure: Either a description or partial address must be specified.")
  }

  @Test
  fun `409 conflict - matching tap authorisation already exists`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val authorisation = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, pi))
    val occurrence = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(authorisation))

    val request = createTapAuthorisationRequest(
      occurrences = listOf(
        createTapOccurrenceRequest(
          releaseAt = occurrence.releaseAt,
          returnBy = occurrence.returnBy,
        ),
      ),
    )
    createTapAuthorisation(pi, request)
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `200 ok tap authorisation created successfully`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    val prisoners = prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapAuthorisationRequest()
    val username = name(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.absenceType?.code).isEqualTo(request.absenceTypeCode)
    assertThat(occurrence.absenceSubType?.code).isEqualTo(request.absenceSubTypeCode)
    assertThat(occurrence.absenceReasonCategory?.code).isEqualTo(request.absenceReasonCategoryCode)
    assertThat(occurrence.absenceReason?.code).isEqualTo(request.absenceReasonCode)
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
        statusCode = TapAuthorisationStatus.Code.APPROVED,
      )
    val username = name(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request.copy(absenceSubTypeCode = "PP", absenceReasonCode = "PC"))

    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.absenceType?.code).isEqualTo("PP")
    assertThat(occurrence.absenceSubType?.code).isEqualTo("PP")
    assertThat(occurrence.absenceReasonCategory?.code).isNull()
    assertThat(occurrence.absenceReason?.code).isEqualTo("PC")
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
    releaseAt: LocalDateTime = LocalDateTime.now().minusHours(3),
    returnBy: LocalDateTime = LocalDateTime.now().plusHours(3),
    location: Location = location(),
    scheduleReference: JsonNode? = null,
  ) = CreateTapOccurrenceRequest(
    releaseAt = releaseAt,
    returnBy = returnBy,
    location = location,
    scheduleReference = scheduleReference,
  )

  private fun location(
    description: String? = name(10),
    address: String? = null,
    postcode: String? = null,
    uprn: String = "${newId()}",
  ): Location = Location(description, address, postcode, uprn)

  private fun createTapAuthorisationRequest(
    absenceTypeCode: String = "SR",
    absenceSubTypeCode: String? = "RDR",
    absenceReasonCategoryCode: String? = "PW",
    absenceReasonCode: String? = "R15",
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    fromDate: LocalDate = LocalDate.now().minusDays(3),
    toDate: LocalDate = LocalDate.now().plusDays(1),
    statusCode: TapAuthorisationStatus.Code = TapAuthorisationStatus.Code.PENDING,
    occurrences: List<CreateTapOccurrenceRequest> = listOf(createTapOccurrenceRequest()),
    notes: String? = null,
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
    notes,
    repeat,
    fromDate,
    toDate,
    contactInformation,
  )

  private fun createTapAuthorisation(
    personIdentifier: String,
    request: CreateTapAuthorisationRequest,
    username: String = DEFAULT_USERNAME,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
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
