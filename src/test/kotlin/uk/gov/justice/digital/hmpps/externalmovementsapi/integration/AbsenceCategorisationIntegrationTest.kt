package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CodedDescription

class AbsenceCategorisationIntegrationTest : IntegrationTest() {
  @Test
  fun `401 unauthorised domain without a valid token`() {
    webTestClient
      .get()
      .uri(REFERENCE_DATA_URL, "any-domain")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `401 unauthorised reference data without a valid token`() {
    webTestClient
      .get()
      .uri(LINKED_RD_URL, "any-domain", "ANY_CODE")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden domain without correct role`() {
    getDomainSpec("any-domain", "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden reference data without correct role`() {
    getLinksSpec("any-domain", "anycode", "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 domain not found if invalid domain`() {
    getDomainSpec("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `404 links not found if invalid domain`() {
    getDomainSpec("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `200 ok - can retrieve absence types`() {
    val rd =
      getDomainSpec("absence-type")
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(rd.domain).isEqualTo(CodedDescription("ABSENCE_TYPE", "Absence type"))
    assertThat(rd.items).containsExactly(
      AbsenceCategorisation("SR", "Standard ROTL (Release on Temporary Licence)", null, ABSENCE_SUB_TYPE),
      AbsenceCategorisation("RR", "Restricted ROTL (Release on Temporary Licence)", null, ABSENCE_SUB_TYPE),
      AbsenceCategorisation("PP", "Police production", null, null),
      AbsenceCategorisation("SE", "Security escort", null, ABSENCE_REASON),
      AbsenceCategorisation("YT", "Youth temporary release", null, ABSENCE_SUB_TYPE),
    )
  }

  @ParameterizedTest
  @MethodSource("referenceDataDomains")
  fun `200 ok - can retrieve reference data domains with correct role`(domain: String) {
    val rd =
      getDomainSpec(domain)
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).isNotEmpty
  }

  @Test
  fun `200 ok - can retrieve linked reasons`() {
    val rd =
      getLinksSpec("absence-sub-type", "RDR")
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(rd.domain).isEqualTo(CodedDescription("ABSENCE_REASON_CATEGORY", "Absence reason category"))
    assertThat(rd.items).containsExactly(
      AbsenceCategorisation("FB", "Accommodation-related", null, null),
      AbsenceCategorisation("ET", "Education or training ", null, null),
      AbsenceCategorisation("R3", "Maintaining family ties", null, null),
      AbsenceCategorisation("PW", "Paid work", null, ABSENCE_REASON),
      AbsenceCategorisation("PAP", "Prisoner apprenticeships pathway", null, null),
      AbsenceCategorisation("UW", "Unpaid work", null, ABSENCE_REASON),
      AbsenceCategorisation("YOTR", "Other temporary release linked to sentence or resettlement plan", null, null),
    )
  }

  @Test
  fun `204 no content when no linked items`() {
    getLinksSpec("absence-type", "PP").expectStatus().isNoContent
  }

  private fun getDomainSpec(
    domain: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(REFERENCE_DATA_URL, domain)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  private fun getLinksSpec(
    domain: String,
    code: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(LINKED_RD_URL, domain, code)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val REFERENCE_DATA_URL = "/absence-categorisation/{domain}"
    const val LINKED_RD_URL = "$REFERENCE_DATA_URL/{code}"

    @JvmStatic
    fun referenceDataDomains() = ReferenceDataDomain.Code.entries.map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }
  }
}
