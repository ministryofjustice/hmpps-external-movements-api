package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.CreateTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.GetTapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.TapAuthorisationModifications
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/temporary-absence-authorisations")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class TapAuthorisationController(
  private val create: CreateTapAuthorisation,
  private val get: GetTapAuthorisation,
  private val modify: TapAuthorisationModifications,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{personIdentifier}")
  fun createTapAuthorisation(
    @PathVariable personIdentifier: String,
    @Valid @RequestBody request: CreateTapAuthorisationRequest,
  ) = create.tapAuthorisation(personIdentifier, request)

  @GetMapping("/{id}")
  fun getTapAuthorisation(
    @PathVariable id: UUID,
    @RequestParam fromDate: LocalDate?,
    @RequestParam toDate: LocalDate?,
  ): TapAuthorisation = get.byId(id, fromDate, toDate)

  @PutMapping("/{id}")
  fun applyActions(@PathVariable id: UUID, @Valid @RequestBody action: AuthorisationAction) {
    modify.apply(id, action)
  }
}
