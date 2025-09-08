package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch

data class PrisonerNumbers(
  val prisonerNumbers: Set<String>,
)

data class PersonSearchRequest(
  val query: String? = null,
  val cellLocationPrefix: String? = null,
  val excludeActiveAllocations: Boolean = false,
)

data class Prisoner(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  val prisonId: String,
  val lastPrisonId: String,
  val cellLocation: String?,
) {
  companion object {
    fun fields() = setOf(
      Prisoner::prisonerNumber.name,
      Prisoner::firstName.name,
      Prisoner::lastName.name,
      Prisoner::prisonId.name,
      Prisoner::lastPrisonId.name,
      Prisoner::cellLocation.name,
    )
  }
}

data class Prisoners(val content: List<Prisoner>) {
  private val map = content.associateBy { it.prisonerNumber }
  operator fun get(prisonerNumber: String): Prisoner? = map[prisonerNumber]
  val personIdentifiers: Set<String> = map.keys
}
