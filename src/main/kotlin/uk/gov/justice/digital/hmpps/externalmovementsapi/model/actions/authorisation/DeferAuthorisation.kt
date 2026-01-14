package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

data class DeferAuthorisation(
  override val reason: String? = null,
) : AuthorisationAction
