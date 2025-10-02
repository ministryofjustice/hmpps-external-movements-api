package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata

data class Organisation(
  val organisationId: Long,
  val organisationName: String,
  val flat: String?,
  val property: String?,
  val street: String?,
  val area: String?,
  val cityCode: String?,
  val cityDescription: String?,
  val countyCode: String?,
  val countyDescription: String?,
  val postcode: String?,
  val countryCode: String?,
  val countryDescription: String?,
)

data class PagedOrganisations(val content: List<Organisation>, val page: PageMetadata)

data class OrganisationDetails(
  val organisationId: Long,
  val organisationName: String,
  val active: Boolean,
  val addresses: List<OrganisationAddressDetails>,
  val emailAddresses: List<OrganisationEmailDetails>,
  val phoneNumbers: List<OrganisationPhoneDetails>,
  val webAddresses: List<OrganisationWebAddressDetails>,
)

data class OrganisationPhoneDetails(
  val phoneType: String,
  val phoneTypeDescription: String,
  val phoneNumber: String,
  val extNumber: String?,
)

data class OrganisationEmailDetails(
  val emailAddress: String,
)

data class OrganisationWebAddressDetails(
  val webAddress: String,
)

data class OrganisationAddressDetails(
  val primaryAddress: Boolean,
  val flat: String?,
  val property: String?,
  val street: String?,
  val area: String?,
  val cityCode: String?,
  val cityDescription: String?,
  val countyCode: String?,
  val countyDescription: String?,
  val postcode: String?,
  val countryCode: String?,
  val countryDescription: String?,
  val noFixedAddress: Boolean,
  val contactPersonName: String?,
  val phoneNumbers: List<OrganisationPhoneDetails>,
)
