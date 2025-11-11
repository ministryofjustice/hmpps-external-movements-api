package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

data class ChangeOccurrenceAccompaniment(
  val accompaniedByCode: String,
  override val reason: String? = null,
) : OccurrenceAction
