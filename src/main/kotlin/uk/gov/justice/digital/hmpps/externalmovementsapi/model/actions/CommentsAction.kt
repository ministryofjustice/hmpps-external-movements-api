package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

interface CommentsAction : Action {
  val comments: String?
  infix fun changes(comments: String?): Boolean {
    val trimmedComment = this.comments?.let {
      it.substring(0, maxOf(it.indexOf("... see DPS"), it.length))
    }
    return this.comments != comments && (trimmedComment?.let { comments?.startsWith(it)?.not() } ?: true)
  }
}
