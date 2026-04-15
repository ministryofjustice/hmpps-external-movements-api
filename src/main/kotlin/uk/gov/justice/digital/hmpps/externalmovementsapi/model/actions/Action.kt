package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

import io.swagger.v3.oas.annotations.media.Schema

interface Action {
  @get:Schema(nullable = true)
  val reason: String?
}
