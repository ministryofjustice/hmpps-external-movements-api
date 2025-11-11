package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

data class AmendOccurrenceNotes(val notes: String, override val reason: String? = null) : OccurrenceAction
