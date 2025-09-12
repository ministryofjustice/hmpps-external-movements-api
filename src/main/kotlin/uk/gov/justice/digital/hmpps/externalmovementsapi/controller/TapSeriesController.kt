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
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.tapseries.CreateTapSeriesRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.CreateTapSeries

@RestController
@RequestMapping("/temporary-absence-series")
class TapSeriesController(private val create: CreateTapSeries) {
  @PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{personIdentifier}")
  fun createTapSeries(@PathVariable personIdentifier: String, @RequestBody request: CreateTapSeriesRequest) = create.tapSeries(personIdentifier, request)
}
