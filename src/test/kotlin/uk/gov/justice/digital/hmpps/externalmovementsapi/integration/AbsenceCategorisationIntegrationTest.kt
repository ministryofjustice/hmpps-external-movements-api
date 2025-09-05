package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisations

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
    getReferenceDataSpec(
      "any-domain",
      "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
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

    assertThat(rd.content).containsExactly(
      AbsenceCategorisation("SR", "Standard ROTL (Release on Temporary Licence)", null, null),
      AbsenceCategorisation("RR", "Restricted ROTL (Release on Temporary Licence)", null, null),
      AbsenceCategorisation("PP", "Police production", null, null),
      AbsenceCategorisation("SE", "Security escort", null, null),
      AbsenceCategorisation("YT", "Youth temporary release", null, null),
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

    assertThat(rd.content).isNotEmpty
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
    fun referenceDataDomains() = ReferenceDataDomain.entries.map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }
  }
}
