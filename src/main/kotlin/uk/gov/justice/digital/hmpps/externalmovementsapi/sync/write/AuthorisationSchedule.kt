package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import java.time.LocalTime

data class AuthorisationSchedule(
  val startTime: LocalTime,
  val returnTime: LocalTime,
) {
  val type: String = "SINGLE"
}
