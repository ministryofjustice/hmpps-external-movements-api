package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

interface CommentsAction : Action {
  val comments: String?
  infix fun changes(comments: String?): Boolean = this.comments != comments
}
