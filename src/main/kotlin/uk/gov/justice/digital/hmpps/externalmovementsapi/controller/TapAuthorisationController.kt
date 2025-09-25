package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.CreateTapAuthorisation

@RestController
@RequestMapping("/temporary-absence-authorisations")
class TapAuthorisationController(private val create: CreateTapAuthorisation) {
  @PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{personIdentifier}")
  fun createTapAuthorisation(@PathVariable personIdentifier: String, @RequestBody request: CreateTapAuthorisationRequest) = create.tapAuthorisation(personIdentifier, request)
}
