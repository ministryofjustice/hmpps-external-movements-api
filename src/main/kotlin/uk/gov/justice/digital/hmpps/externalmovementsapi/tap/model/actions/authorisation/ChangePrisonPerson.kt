package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation

data class ChangePrisonPerson(
  val personIdentifier: String,
  val prisonCode: String,
) : AuthorisationAction
