package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations

import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location

fun OrganisationDetails.asLocation(type: ReferenceData): Location {
  val primaryAddress = addresses.firstOrNull { it.primaryAddress }
  return Location(
    type.asCodedDescription(),
    organisationName,
    active,
    primaryAddress?.asLocationAddress(),
    primaryAddress?.let { contactDetails(it) },
    "$organisationId",
  )
}

private fun OrganisationDetails.contactDetails(primaryAddress: OrganisationAddressDetails): Location.Contact? = primaryAddress.contactPersonName?.let { contactName ->
  Location.Contact(contactName, phoneNumbers(primaryAddress))
}

private fun OrganisationDetails.phoneNumbers(pa: OrganisationAddressDetails): List<Location.Contact.Phone> = pa.phoneNumbers.map { it.asLocationPhone(true) } + phoneNumbers.map { it.asLocationPhone(false) }

private fun OrganisationPhoneDetails.asLocationPhone(addressSpecific: Boolean) = Location.Contact.Phone(
  phoneTypeDescription,
  phoneNumber,
  extNumber,
  addressSpecific,
)

private fun OrganisationAddressDetails.asLocationAddress() = Location.Address(
  listOfNotNull(flat, property).takeIf { it.isNotEmpty() }?.joinToString(", "),
  street,
  area,
  cityDescription,
  countyDescription,
  countryDescription,
  postcode,
)
