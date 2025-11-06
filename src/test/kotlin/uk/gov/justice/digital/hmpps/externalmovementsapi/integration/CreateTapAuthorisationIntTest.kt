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
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
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
  @Autowired private val tasOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by tasOperations,
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
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
    val request = createTapAuthorisationRequest()
    val username = name(8)
    val res = createTapAuthorisation(pi, request, username).successResponse<ReferenceId>(HttpStatus.CREATED)

    assertThat(res.id).isNotNull
    val saved = requireNotNull(findTemporaryAbsenceAuthorisation(res.id))
    saved.verifyAgainst(pi, request)
    assertThat(saved.approvedAt).isNull()
    assertThat(saved.approvedBy).isNull()
    assertThat(saved.submittedBy).isEqualTo(username)
    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.addedBy).isEqualTo(username)

    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(
        TemporaryAbsenceAuthorisation::class.simpleName!!,
        TemporaryAbsenceOccurrence::class.simpleName!!,
      ),
      ExternalMovementContext.get().copy(username = username),
    )

    verifyEvents(saved, setOf())
  }

  @Test
  fun `200 ok tap authorisation created successfully using default links`() {
    val prisonCode = prisonCode()
    val pi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(pi))
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
    assertThat(saved.approvedAt).isNotNull()
    assertThat(saved.approvedBy).isEqualTo(username)
    assertThat(saved.submittedBy).isEqualTo(username)
    val occurrence = findForAuthorisation(saved.id).first()
    occurrence.verifyAgainst(pi, request.occurrences.first(), request)
    assertThat(occurrence.addedBy).isEqualTo(username)

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
      listOf(TemporaryAbsenceRescheduled(pi, occurrence.id), TemporaryAbsenceAuthorised(pi, saved.id)).toSet(),
    )
  }

  private fun createTapOccurrenceRequest(
    releaseAt: LocalDateTime = LocalDateTime.now().minusDays(7),
    returnBy: LocalDateTime = LocalDateTime.now(),
    accompaniedByCode: String = "L",
    transportCode: String = "OD",
    contactInformation: String? = null,
    notes: String? = "Some notes about the authorisation",
    location: Location = location(),
    scheduleReference: JsonNode? = null,
  ) = CreateTapOccurrenceRequest(
    releaseAt = releaseAt,
    returnBy = returnBy,
    accompaniedByCode = accompaniedByCode,
    transportCode = transportCode,
    contactInformation = contactInformation,
    notes = notes,
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
    fromDate: LocalDate = LocalDate.now().minusDays(7),
    toDate: LocalDate = LocalDate.now().minusDays(1),
    statusCode: TapAuthorisationStatus.Code = TapAuthorisationStatus.Code.PENDING,
    occurrences: List<CreateTapOccurrenceRequest> = listOf(createTapOccurrenceRequest()),
    notes: String? = null,
    repeat: Boolean = false,
  ) = CreateTapAuthorisationRequest(
    absenceTypeCode,
    absenceSubTypeCode,
    absenceReasonCategoryCode,
    absenceReasonCode,
    occurrences,
    statusCode,
    notes,
    repeat,
    fromDate,
    toDate,
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
