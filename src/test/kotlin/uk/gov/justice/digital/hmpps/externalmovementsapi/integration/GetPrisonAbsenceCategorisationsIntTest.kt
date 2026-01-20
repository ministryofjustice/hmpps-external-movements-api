package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.PrisonAbsenceCategorisations

class GetPrisonAbsenceCategorisationsIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_PRISON_CATEGORISATIONS_URL, prisonCode())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getPacs(prisonCode(), "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `200 ok - empty lists returned when no tap data available`() {
    val prisonCode = prisonCode()
    val response = getPacs(prisonCode).successResponse<PrisonAbsenceCategorisations>()

    assertThat(response.types).isEmpty()
    assertThat(response.subTypes).isEmpty()
    assertThat(response.reasonCategories).isEmpty()
    assertThat(response.reasons).isEmpty()
  }

  @Test
  fun `200 ok - returns all categorisation information`() {
    val prisonCode = prisonCode()
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode))
    givenTemporaryAbsenceOccurrence(
      temporaryAbsenceOccurrence(
        auth,
        absenceType = "YT",
        absenceSubType = "YTRA",
        absenceReasonCategory = null,
        absenceReason = "YRDR",
      ),
    )

    val response = getPacs(prisonCode).successResponse<PrisonAbsenceCategorisations>()

    println(response)

    assertThat(response.types).containsExactly(
      CodedDescription(code = "SR", description = "Standard ROTL (release on temporary licence)"),
      CodedDescription(code = "YT", description = "Youth temporary release"),
    )
    assertThat(response.subTypes).containsExactly(
      CodedDescription(code = "YTRA", description = "Accommodation"),
      CodedDescription(code = "RDR", description = "RDR (resettlement day release)"),
    )
    assertThat(response.reasonCategories).containsExactly(CodedDescription(code = "PW", description = "Paid work"))
    assertThat(response.reasons).containsExactly(
      CodedDescription(code = "R15", description = "IT and communication"),
      // Special case - Youth prepended for filter as per MIA-1476
      CodedDescription(code = "YRDR", description = "Youth - RDR (resettlement day release)"),
    )
  }

  private fun getPacs(
    prisonCode: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_PRISON_CATEGORISATIONS_URL, prisonCode)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_PRISON_CATEGORISATIONS_URL = "/prisons/{prisonIdentifier}/absence-categorisations"
  }
}
