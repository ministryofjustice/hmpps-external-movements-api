package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

data class ChangeOccurrenceTransport(
  val transportCode: String,
  override val reason: String? = null,
) : OccurrenceAction
