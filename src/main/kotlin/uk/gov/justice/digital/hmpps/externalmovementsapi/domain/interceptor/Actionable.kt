package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import java.util.UUID

fun interface Actionable {
  fun actions(): List<Action>
}

interface Action {
  val type: String
  val reason: String?
  val id: UUID
}
