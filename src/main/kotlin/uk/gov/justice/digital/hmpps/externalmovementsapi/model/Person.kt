package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import java.time.LocalDate

data class Person(
  val personIdentifier: String,
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val cellLocation: String?,
) {
  companion object {
    fun unknown(personIdentifier: String) = Person(personIdentifier, firstName = "", lastName = "", LocalDate.EPOCH, cellLocation = null)
  }
}
