package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence

data class ChangeOccurrenceContactInformation(
  val information: String,
  override val reason: String? = null,
) : OccurrenceAction
