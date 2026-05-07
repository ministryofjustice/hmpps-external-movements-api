package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.OpenApiTags.INTEGRATIONS
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationResponses
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationUrlBuilder.authorisationOccurrencesUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationUrlBuilder.authorisationUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationUrlBuilder.occurrenceMovementsUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationUrlBuilder.occurrenceUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.IntegrationRetriever
import java.util.UUID

@Tag(name = INTEGRATIONS)
@RestController
@RequestMapping(value = ["integrations"])
@PreAuthorize("hasAnyRole('${Roles.EXTERNAL_MOVEMENTS_RO}', '${Roles.EXTERNAL_MOVEMENTS_RW}', '${Roles.TEMPORARY_ABSENCE_RO}', '${Roles.TEMPORARY_ABSENCE_RW}')")
class IntegrationController(
  private val retrieve: IntegrationRetriever,
) {
  @GetMapping("/temporary-absence-authorisations/{id}")
  fun authorisation(@PathVariable id: UUID): IntegrationResponse<IntegrationAuthorisation> = IntegrationResponse(retrieve.authorisation(id), null, authorisationOccurrencesUrl(id))

  @GetMapping("/temporary-absence-authorisations/{id}/occurrences")
  fun occurrencesForAuthorisation(@PathVariable id: UUID): IntegrationResponses<IntegrationOccurrence> {
    val previousUrl = authorisationUrl(id)
    return retrieve.occurrencesForAuthorisation(id)
      .map { IntegrationResponse(it, previousUrl, occurrenceMovementsUrl(it.id)) }
      .let { IntegrationResponses(it, previousUrl) }
  }

  @GetMapping("/temporary-absence-occurrences/{id}")
  fun occurrence(@PathVariable id: UUID): IntegrationResponse<IntegrationOccurrence> {
    val occ = retrieve.occurrence(id)
    return IntegrationResponse(occ, authorisationUrl(occ.authorisationId), occurrenceMovementsUrl(occ.id))
  }

  @GetMapping("/temporary-absence-occurrences/{id}/movements")
  fun movementsForOccurrence(@PathVariable id: UUID): IntegrationResponses<IntegrationMovement> {
    val previousUrl = occurrenceUrl(id)
    return retrieve.movementsForOccurrence(id)
      .map { IntegrationResponse(it, previousUrl, null) }
      .let { IntegrationResponses(it, previousUrl) }
  }

  @GetMapping("/temporary-absence-movements/{id}")
  fun movement(@PathVariable id: UUID): IntegrationResponse<IntegrationMovement> {
    val movement = retrieve.movement(id)
    return IntegrationResponse(movement, movement.occurrenceId?.let { occurrenceUrl(it) }, null)
  }
}
