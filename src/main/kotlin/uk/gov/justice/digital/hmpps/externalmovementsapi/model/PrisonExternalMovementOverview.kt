package uk.gov.justice.digital.hmpps.externalmovementsapi.model

data class PrisonExternalMovementOverview(
  val configuration: Configuration,
  val tapOverview: TapOverview,
) {
  data class Configuration(
    val tapEnabled: Boolean,
    val courtMovementsEnabled: Boolean,
    val transfersEnabled: Boolean,
    val releasesEnabled: Boolean,
  ) {
    companion object {
      val DEFAULT = Configuration(
        tapEnabled = true,
        courtMovementsEnabled = false,
        transfersEnabled = false,
        releasesEnabled = false,
      )
    }
  }

  data class TapOverview(
    val leavingToday: Int,
    val returningToday: Int,
    val approvalsRequired: Int,
  )
}
