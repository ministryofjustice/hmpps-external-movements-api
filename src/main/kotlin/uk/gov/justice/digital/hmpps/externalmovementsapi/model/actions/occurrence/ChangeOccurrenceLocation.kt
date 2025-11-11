package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

data class ChangeOccurrenceLocation(
  val location: Location,
  override val reason: String? = null,
) : OccurrenceAction
