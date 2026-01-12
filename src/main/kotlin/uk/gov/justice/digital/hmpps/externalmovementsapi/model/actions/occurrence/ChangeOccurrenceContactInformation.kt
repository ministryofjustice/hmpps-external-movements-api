package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

data class ChangeOccurrenceContactInformation(
  val information: String,
  override val reason: String? = null,
) : OccurrenceAction
