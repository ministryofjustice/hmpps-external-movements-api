package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.referencedata

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
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
      getReferenceDataSpec("tap-authorisation-status")
        .expectStatus()
        .isOk
        .expectBody<ReferenceDataResponse>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).containsExactly(
      CodedDescription("PENDING", "Pending"),
      CodedDescription("APPROVED", "Approved"),
      CodedDescription("CANCELLED", "Cancelled"),
      CodedDescription("DENIED", "Denied"),
    )
  }

  @Test
  fun `can get reference data with hint text`() {
    val rd =
      getReferenceDataSpec("location-type")
        .expectStatus()
        .isOk
        .expectBody<ReferenceDataResponse>()
        .returnResult()
        .responseBody!!

    assertThat(rd.items).containsExactly(
      CodedDescription("CORP", "Business or community", "These are places of business or community settings such as workplaces, approved premises, hospitals and colleges. This does not have to be a specific addresses but can be a whole area (for example, York)."),
      CodedDescription("OFF", "Personal", "These are locations personal to the prisoner. For example, their home address, relatives’ addresses or partner’s address."),
      CodedDescription("AGY", "Official", "These are official locations associated with the justice system. For example, probation offices, police stations and courts."),
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
    fun referenceDataDomains() = listOf(ACCOMPANIED_BY, TRANSPORT, TAP_AUTHORISATION_STATUS).map { rdd ->
      Arguments.of(rdd.name)
      Arguments.of(rdd.name.lowercase())
      Arguments.of(rdd.name.lowercase().replace("_", "-"))
    }
  }
}
