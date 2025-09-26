package uk.gov.justice.digital.hmpps.externalmovementsapi.model

data class Person(val personIdentifier: String, val firstName: String, val lastName: String, val cellLocation: String?) {
  companion object {
    fun unknown(personIdentifier: String) = Person(personIdentifier, firstName = "", lastName = "", cellLocation = null)
  }
}
