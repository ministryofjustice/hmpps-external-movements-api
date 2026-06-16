package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.CreateOccurrenceRequest

data class CreateOccurrences(
  @Valid
  @NotEmpty
  val occurrences: List<CreateOccurrenceRequest>,
) : AuthorisationAction
