package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

interface CommentsAction : Action {
  val comments: String?
  infix fun changes(comments: String?): Boolean {
    val trimmedComment = this.comments?.let {
      val endIndex = it.indexOf("... see DPS")
      it.substring(0, if (endIndex > 0) endIndex else it.length)
    }
    return this.comments != comments && (trimmedComment?.let { comments?.startsWith(it)?.not() } ?: true)
  }
}
