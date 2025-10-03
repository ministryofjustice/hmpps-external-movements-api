package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.search.locations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.OrganisationsExtension.Companion.organisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.OrganisationsServer.Companion.organisationDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription

class GetLocationsByIdIntTest : IntegrationTest() {

  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(GET_LOCATION_BY_ID_URL, LocationType.Code.CORP, "${newId()}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `403 forbidden without correct role`() {
    getLocation(LocationType.Code.AGY, "${newId()}", role = "ROLE_ANY__OTHER_RW")
      .expectStatus().isForbidden
  }

  @Test
  fun `404 not found for AGY and OFF`() {
    listOf(LocationType.Code.AGY, LocationType.Code.OFF).forEach {
      getLocation(it, "${newId()}").expectStatus().isNotFound
    }
  }

  @Test
  fun `200 ok for CORP locations`() {
    val organisation = organisationDetails()
    organisations.byId(organisation)
    val res = getLocation(LocationType.Code.CORP, "${organisation.organisationId}").successResponse<Location>()
    res.verifyAgainst(organisation)
  }

  private fun Location.verifyAgainst(organisation: OrganisationDetails) {
    assertThat(type).isEqualTo(CodedDescription("CORP", "Business or community", "These are places of business or community settings such as workplaces, approved premises, hospitals and colleges. This does not have to be a specific addresses but can be a whole area (for example, York)."))
    assertThat(description).isEqualTo(organisation.organisationName)
    assertThat(contactInformation?.names?.first()).isEqualTo(organisation.addresses.firstOrNull { it.primaryAddress }?.contactPersonName)
    val orgPhoneCount = organisation.phoneNumbers.size +
      (organisation.addresses.firstOrNull { it.primaryAddress }?.phoneNumbers?.size ?: 0)
    assertThat(contactInformation?.phones).hasSize(orgPhoneCount)
  }

  private fun getLocation(
    typeCode: LocationType.Code,
    id: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(GET_LOCATION_BY_ID_URL, typeCode, id)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val GET_LOCATION_BY_ID_URL = "/locations/{typeCode}/{id}"
  }
}
