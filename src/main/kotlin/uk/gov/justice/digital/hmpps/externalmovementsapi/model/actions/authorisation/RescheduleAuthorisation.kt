package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import java.time.LocalDate

data class RescheduleAuthorisation(
  val from: LocalDate,
  val to: LocalDate,
  val repeat: Boolean,
  override val reason: String? = null,
) : AuthorisationAction
