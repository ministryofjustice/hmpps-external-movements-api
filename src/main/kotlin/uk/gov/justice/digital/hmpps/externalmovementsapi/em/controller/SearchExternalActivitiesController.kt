package uk.gov.justice.digital.hmpps.externalmovementsapi.em.controller

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags.INTEGRATIONS
import uk.gov.justice.digital.hmpps.externalmovementsapi.em.model.ExternalActivities
import uk.gov.justice.digital.hmpps.externalmovementsapi.em.model.SearchExternalActivitiesRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.em.service.SearchScheduledMovements

@Tag(name = INTEGRATIONS)
@RestController
@PreAuthorize("hasAnyRole('${Roles.EXTERNAL_MOVEMENTS_RO}', '${Roles.EXTERNAL_MOVEMENTS_RW}')")
@RequestMapping("/search/prisons/{prisonCode}/external-activities")
class SearchExternalActivitiesController(private val search: SearchScheduledMovements) {
  @PostMapping
  fun externalActivities(
    @PathVariable prisonCode: String,
    @Valid @RequestBody request: SearchExternalActivitiesRequest,
  ): ExternalActivities = search.externalActivities(prisonCode, request)
}
