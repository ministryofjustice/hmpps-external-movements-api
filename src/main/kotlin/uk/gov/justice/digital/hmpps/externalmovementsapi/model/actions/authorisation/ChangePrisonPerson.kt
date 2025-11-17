package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

data class ChangePrisonPerson(
  val personIdentifier: String,
  val prisonCode: String,
  override val reason: String? = null,
) : AuthorisationAction
