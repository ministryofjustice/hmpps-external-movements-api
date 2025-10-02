package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search.locations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.Organisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.PagedOrganisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.OrganisationsExtension.Companion.organisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.OrganisationsServer.Companion.organisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata

class SearchLocationsIntTest : IntegrationTest() {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(SEARCH_LOCATIONS_URL, LocationType.Code.CORP)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    searchLocations(
      LocationType.Code.AGY,
      "Any String",
      role = "ROLE_ANY__OTHER_RW",
    ).expectStatus().isForbidden
  }

  @Test
  fun `200 ok empty list for AGY and OFF`() {
    listOf(LocationType.Code.AGY, LocationType.Code.OFF).forEach { locationType ->
      val res = searchLocations(locationType, "Any")
        .expectStatus().isOk
        .successResponse<LocationSearchResponse>()

      assertThat(res.content).hasSize(0)
      assertThat(res.metadata.totalElements).isEqualTo(0)
    }
  }

  @Test
  fun `200 ok - maps organisation provided`() {
    val organisation = organisation()
    organisations.find(organisation.organisationName, PagedOrganisations(listOf(organisation), PageMetadata(1)))
    val res = searchLocations(LocationType.Code.CORP, organisation.organisationName)
      .expectStatus().isOk
      .successResponse<LocationSearchResponse>()

    assertThat(res.content).hasSize(1)
    assertThat(res.metadata.totalElements).isEqualTo(1)
    res.content.first().verifyAgainst(organisation)
  }

  private fun LocationResult.verifyAgainst(organisation: Organisation) {
    assertThat(postcode).isEqualTo(organisation.postcode)
    assertThat(name).isEqualTo(organisation.organisationName)
  }

  private fun searchLocations(
    typeCode: LocationType.Code,
    query: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri { uri ->
      uri.path(SEARCH_LOCATIONS_URL)
      uri.queryParam("query", query)
      uri.build(typeCode)
    }
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val SEARCH_LOCATIONS_URL = "/search/locations/{typeCode}"
  }
}
