package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import java.util.UUID

data class ScheduledMovementDetail(
  val uiUrl: String,
  val requiredRoles: Set<String>,
) {
  companion object {
    fun buildUiUrl(uiBaseUrl: String, occurrenceId: UUID): String = "$uiBaseUrl/temporary-absences/$occurrenceId"
  }
}

data class LocationDescription(val description: String)
data class ScheduledMovementDescription(val full: String, val short: String)
