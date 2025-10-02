package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.newId
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.postcode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.telephone
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.json.Json
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.Organisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationAddressDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationEmailDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationPhoneDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationWebAddressDetails
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.OrganisationsClient.Companion.ORGANISATION_SEARCH_URL
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations.PagedOrganisations

class OrganisationsServer : WireMockServer(8900) {
  fun find(query: String, response: PagedOrganisations) {
    stubFor(
      get(urlPathEqualTo("/${ORGANISATION_SEARCH_URL}"))
        .withQueryParam("name", equalTo(query))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(Json.mapper.writeValueAsString(response))
            .withStatus(HttpStatus.OK.value()),
        ),
    )
  }

  fun byId(organisation: OrganisationDetails) {
    stubFor(
      get(urlPathEqualTo("/organisation/${organisation.organisationId}"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(Json.mapper.writeValueAsString(organisation))
            .withStatus(HttpStatus.OK.value()),
        ),
    )
  }

  companion object {
    fun organisation(
      organisationId: Long = newId(),
      organisationName: String = "Organisation ${name(8)}",
      flat: String? = null,
      property: String? = null,
      street: String? = "${name(6)} Street",
      area: String? = null,
      cityCode: String? = prisonCode(),
      cityDescription: String? = name(8),
      countyCode: String? = prisonCode(),
      countyDescription: String? = name(12),
      postcode: String? = postcode(),
      countryCode: String? = prisonCode(),
      countryDescription: String? = name(10),
    ) = Organisation(
      organisationId,
      organisationName,
      flat,
      property,
      street,
      area,
      cityCode,
      cityDescription,
      countyCode,
      countyDescription,
      postcode,
      countryCode,
      countryDescription,
    )

    fun organisationDetails(
      organisationId: Long = newId(),
      organisationName: String = "Organisation ${name(8)}",
      active: Boolean = true,
      addresses: List<OrganisationAddressDetails> = listOf(addressDetails()),
      emails: List<OrganisationEmailDetails> = listOf(),
      phones: List<OrganisationPhoneDetails> = listOf(phoneDetails()),
      webAddresses: List<OrganisationWebAddressDetails> = listOf(),
    ) = OrganisationDetails(
      organisationId,
      organisationName,
      active,
      addresses,
      emails,
      phones,
      webAddresses,
    )

    fun addressDetails(
      primaryAddress: Boolean = true,
      flat: String? = null,
      property: String? = null,
      street: String? = "${name(6)} Street",
      area: String? = null,
      cityCode: String? = prisonCode(),
      cityDescription: String? = name(8),
      countyCode: String? = prisonCode(),
      countyDescription: String? = name(12),
      postcode: String? = postcode(),
      countryCode: String? = prisonCode(),
      countryDescription: String? = name(10),
      noFixedAddress: Boolean = false,
      contactPersonName: String? = name(7),
      phoneNumbers: List<OrganisationPhoneDetails> = listOf(phoneDetails()),
    ) = OrganisationAddressDetails(
      primaryAddress,
      flat,
      property,
      street,
      area,
      cityCode,
      cityDescription,
      countyCode,
      countyDescription,
      postcode,
      countryCode,
      countryDescription,
      noFixedAddress,
      contactPersonName,
      phoneNumbers,
    )

    fun phoneDetails(
      phoneNumber: String = telephone(),
      extNumber: String? = null,
      phoneType: String = "PhoneType",
      phoneTypeDescription: String = "Description of phone type",
    ) = OrganisationPhoneDetails(phoneType, phoneTypeDescription, phoneNumber, extNumber)
  }
}

class OrganisationsExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val organisations = OrganisationsServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    organisations.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    organisations.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    organisations.stop()
  }
}
