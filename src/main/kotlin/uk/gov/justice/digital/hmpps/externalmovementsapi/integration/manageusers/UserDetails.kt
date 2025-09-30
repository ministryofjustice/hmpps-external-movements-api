package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers

data class UserDetails(
  val username: String,
  val name: String,
)

fun String.asSystemUser() = UserDetails(this, "User $this")
