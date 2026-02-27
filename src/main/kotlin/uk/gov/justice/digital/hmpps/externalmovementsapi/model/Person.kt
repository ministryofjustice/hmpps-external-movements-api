package uk.gov.justice.digital.hmpps.externalmovementsapi.model

data class Person(
  val personIdentifier: String,
  val firstName: String,
  val lastName: String,
  val prisonCode: String?,
  val cellLocation: String?,
)
