package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.referencedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.EXTERNAL_MOVEMENTS_UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RO
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles.TEMPORARY_ABSENCE_RW
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisationFilters
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisationFilters.Option

class AbsenceCategorisationFiltersIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised domain without a valid token`() {
    webTestClient
      .get()
      .uri(ABSENCE_CATEGORISATION_FILTERS_URL)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [EXTERNAL_MOVEMENTS_RO, TEMPORARY_ABSENCE_RO, TEMPORARY_ABSENCE_RW, "ROLE_ANY__OTHER_RW"])
  fun `403 forbidden domain without correct role`(role: String) {
    getFilters(role).expectStatus().isForbidden
  }

  @Test
  fun `200 ok - can retrieve categorisation filters`() {
    val res = getFilters().successResponse<AbsenceCategorisationFilters>()

    assertThat(res.types).containsExactly(
      Option(domainCode = ABSENCE_REASON, code = "PC", description = "Police production"),
      Option(domainCode = ABSENCE_TYPE, code = "RR", description = "Restricted ROTL (release on temporary licence)"),
      Option(domainCode = ABSENCE_TYPE, code = "SE", description = "Security escort"),
      Option(domainCode = ABSENCE_TYPE, code = "SR", description = "Standard ROTL (release on temporary licence)"),
      Option(domainCode = ABSENCE_TYPE, code = "YT", description = "Youth temporary release"),
    )

    assertThat(res.subTypes).containsExactly(
      Option(domainCode = ABSENCE_REASON, code = "3", description = "CRL (childcare resettlement licence)"),
      Option(domainCode = ABSENCE_SUB_TYPE, code = "RDR", description = "RDR (resettlement day release)"),
      Option(domainCode = ABSENCE_REASON, code = "RO", description = "ROR (resettlement overnight release)"),
      Option(domainCode = ABSENCE_SUB_TYPE, code = "SPL", description = "SPL (special purpose licence)"),
      Option(domainCode = ABSENCE_SUB_TYPE, code = "YTRA", description = "Youth - Accommodation"),
      Option(domainCode = ABSENCE_SUB_TYPE, code = "YTRC", description = "Youth - Case work and transitions"),
      Option(domainCode = ABSENCE_SUB_TYPE, code = "YTRE", description = "Youth - Education, training and employment"),
      Option(domainCode = ABSENCE_SUB_TYPE, code = "YTRF", description = "Youth - Family"),
    )

    assertThat(res.reasons).containsExactly(
      Option(domainCode = ABSENCE_REASON, code = "FB", description = "Accommodation-related"),
      Option(domainCode = ABSENCE_REASON, code = "LTX", description = "Court, legal, police or prison transfer"),
      Option(domainCode = ABSENCE_REASON, code = "C3", description = "Death or funeral"),
      Option(domainCode = ABSENCE_REASON, code = "ET", description = "Education or training"),
      Option(domainCode = ABSENCE_REASON, code = "C6", description = "Inpatient medical or dental appointment"),
      Option(domainCode = ABSENCE_REASON, code = "R3", description = "Maintaining family ties"),
      Option(domainCode = ABSENCE_REASON, code = "4", description = "Other compassionate reason"),
      Option(domainCode = ABSENCE_REASON, code = "SE", description = "Other security escort"),
      Option(domainCode = ABSENCE_REASON, code = "20", description = "Other temporary release linked to sentence or resettlement plan"),
      Option(domainCode = ABSENCE_REASON, code = "C5", description = "Outpatient medical or dental appointment"),
      Option(domainCode = ABSENCE_REASON, code = "OPA", description = "Outside prison activity"),
      Option(domainCode = ABSENCE_REASON_CATEGORY, code = "PW", description = "Paid work"),
      Option(domainCode = ABSENCE_REASON, code = "PAP", description = "Prisoner apprenticeships pathway"),
      Option(domainCode = ABSENCE_REASON_CATEGORY, code = "UW", description = "Unpaid work"),
      Option(domainCode = ABSENCE_REASON, code = "C7", description = "Visit a dying relative"),
      Option(domainCode = ABSENCE_REASON, code = "C4", description = "Wedding or civil ceremony"),
      Option(domainCode = ABSENCE_REASON, code = "YMI", description = "Youth - Meeting and interviews"),
      Option(domainCode = ABSENCE_REASON, code = "YOTR", description = "Youth - Other temporary release linked to sentence or resettlement plan"),
      Option(domainCode = ABSENCE_REASON, code = "YRDR", description = "Youth - RDR (resettlement day release)"),
    )

    assertThat(res.workTypes).containsExactly(
      Option(domainCode = ABSENCE_REASON, code = "R16", description = "Paid work - Agriculture and horticulture"),
      Option(domainCode = ABSENCE_REASON, code = "R12", description = "Paid work - Catering and hospitality"),
      Option(domainCode = ABSENCE_REASON, code = "R14", description = "Paid work - Construction and recycling"),
      Option(domainCode = ABSENCE_REASON, code = "R15", description = "Paid work - IT and communication"),
      Option(domainCode = ABSENCE_REASON, code = "R11", description = "Paid work - Manufacturing"),
      Option(domainCode = ABSENCE_REASON, code = "R17", description = "Paid work - Other"),
      Option(domainCode = ABSENCE_REASON, code = "R10", description = "Paid work - Retail and wholesale"),
      Option(domainCode = ABSENCE_REASON, code = "R13", description = "Paid work - Transportation and storage"),
      Option(domainCode = ABSENCE_REASON, code = "R24", description = "Unpaid work - Agriculture and horticulture"),
      Option(domainCode = ABSENCE_REASON, code = "R20", description = "Unpaid work - Catering and hospitality"),
      Option(domainCode = ABSENCE_REASON, code = "R22", description = "Unpaid work - Construction and recycling"),
      Option(domainCode = ABSENCE_REASON, code = "R23", description = "Unpaid work - IT and communication"),
      Option(domainCode = ABSENCE_REASON, code = "R19", description = "Unpaid work - Manufacturing"),
      Option(domainCode = ABSENCE_REASON, code = "R25", description = "Unpaid work - Other"),
      Option(domainCode = ABSENCE_REASON, code = "R18", description = "Unpaid work - Retail and wholesale"),
      Option(domainCode = ABSENCE_REASON, code = "R21", description = "Unpaid work - Transportation and storage"),
    )
  }

  private fun getFilters(
    role: String? = EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(ABSENCE_CATEGORISATION_FILTERS_URL)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val ABSENCE_CATEGORISATION_FILTERS_URL = "/absence-categorisation/filters"
  }
}
