package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.ReferenceDataResponse

class ReferenceDataIntegrationTest : IntegrationTest() {
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
  fun `404 if invalid domain`() {
    getReferenceDataSpec("any-domain").expectStatus().isNotFound
  }

  @Test
  fun `sorts by sequence number`() {
    val rd =
      getReferenceDataSpec("tap-status")
        .expectStatus()
        .isOk
        .expectBody<ReferenceDataResponse>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).containsExactly(
      CodedDescription("PEN", "Pending"),
      CodedDescription("APP-SCH", "Approved (Scheduled)"),
      CodedDescription("APP-UNSCH", "Approved (Unscheduled)"),
      CodedDescription("CANC", "Cancelled"),
      CodedDescription("DEN", "Denied"),
    )
  }

  @ParameterizedTest
  @MethodSource("referenceDataDomains")
  fun `200 ok - can retrieve reference data domains with correct role`(domain: String) {
    val rd =
      getReferenceDataSpec(domain)
        .expectStatus()
        .isOk
        .expectBody<ReferenceDataResponse>()
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
    const val REFERENCE_DATA_URL = "/reference-data/{domain}"

    @JvmStatic
    fun referenceDataDomains() = listOf(ACCOMPANIED_BY, TRANSPORT, TAP_STATUS).map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }
  }
}
