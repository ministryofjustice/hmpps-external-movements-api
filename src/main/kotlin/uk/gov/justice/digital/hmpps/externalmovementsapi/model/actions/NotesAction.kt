package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

interface NotesAction : Action {
  val notes: String
  fun changes(notes: String?): Boolean = this.notes != notes
}
