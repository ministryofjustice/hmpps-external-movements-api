package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain.Type.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain.Type.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CodedDescription

class AbsenceCategorisationIntegrationTest : IntegrationTest() {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(REFERENCE_DATA_URL, "any-domain")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getReferenceDataSpec("any-domain", "ROLE_ANY__OTHER_RW").expectStatus().isForbidden
  }

  @Test
  fun `404 not found if invalid domain`() {
    getReferenceDataSpec("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `200 ok - can retrieve absence types`() {
    val rd =
      getReferenceDataSpec("absence-type")
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
      getReferenceDataSpec(domain)
        .expectStatus()
        .isOk
        .expectBody<AbsenceCategorisations>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).isNotEmpty
  }

  private fun getReferenceDataSpec(
    domain: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(REFERENCE_DATA_URL, domain)
    .headers(setAuthorisation(username = DEFAULT_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val REFERENCE_DATA_URL = "/absence-categorisation/{domain}"

    @JvmStatic
    fun referenceDataDomains() = ReferenceDataDomain.Type.entries.map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }
  }
}
