package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.util.SequencedSet

data class Location(
  val type: CodedDescription,
  val description: String,
  val isActive: Boolean,
  val address: Address?,
  val contactInformation: ContactInformation?,
  val id: String?,
) {

  data class Address(
    val premise: String?,
    val street: String?,
    val area: String?,
    val city: String?,
    val county: String?,
    val country: String?,
    val postcode: String?,
  )

  data class ContactInformation(
    val names: SequencedSet<String> = linkedSetOf(),
    val phones: List<Phone> = emptyList(),
    val emails: List<EmailAddress> = emptyList(),
    val webAddresses: List<WebAddress> = emptyList(),
  ) {
    data class Phone(
      val type: String,
      val number: String,
      val extension: String?,
      val isAddressSpecific: Boolean,
    )

    data class EmailAddress(val email: String, val isAddressSpecific: Boolean)
    data class WebAddress(val web: String, val isAddressSpecific: Boolean)
  }

  companion object {
    fun unknown(type: CodedDescription) = Location(type, "Location not found", false, null, null, null)
  }
}
