package uk.gov.justice.digital.hmpps.externalmovementsapi.model.action

import jakarta.validation.Valid

interface Actions<T : Action> {
  @get:Valid
  val actions: List<T>
  val reason: String?
}
