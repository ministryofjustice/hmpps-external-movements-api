package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags.UI
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.GetPrisonOverview
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.TapLocationManager

@Tag(name = UI)
@RestController
@RequestMapping("prisons")
@PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
class PrisonController(private val overview: GetPrisonOverview, private val locations: TapLocationManager) {
  @GetMapping("/{prisonCode}/external-movements/overview")
  fun getPrisonOverview(@PathVariable prisonCode: String): PrisonExternalMovementOverview = overview.forPrison(prisonCode)

  @GetMapping("{prisonCode}/temporary-absence-locations")
  fun getPrisonTapLocations(@PathVariable prisonCode: String): TapLocations = locations.getForPrison(prisonCode)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping("{prisonCode}/temporary-absence-locations")
  fun setPrisonTapLocations(@PathVariable prisonCode: String, @Valid @RequestBody request: TapLocations) {
    locations.setForPrison(prisonCode, request)
  }
}
