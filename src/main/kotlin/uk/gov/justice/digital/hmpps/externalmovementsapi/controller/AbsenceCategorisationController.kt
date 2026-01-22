package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisationFilters
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.AbsenceCategorisationRetriever

@RestController
@RequestMapping("absence-categorisation")
@PreAuthorize("hasAnyRole('${Roles.TEMPORARY_ABSENCE_RO}', '${Roles.TEMPORARY_ABSENCE_RW}')")
class AbsenceCategorisationController(private val acRetriever: AbsenceCategorisationRetriever) {
  @GetMapping("/{domain}")
  fun getAbsenceCategorisationDomain(
    @Parameter(
      description = "The reference data domain required. This is case insensitive.",
      schema =
      Schema(
        type = "string",
        allowableValues = [
          "absence-reason",
          "absence-reason-category",
          "absence-sub-type",
          "absence-type",
        ],
      ),
    )
    @PathVariable domain: String,
  ): AbsenceCategorisations = acRetriever.findByDomain(ReferenceDataDomain.Code.of(domain))

  @GetMapping("/{domain}/{code}")
  fun getAbsenceCategorisationOptions(
    @Parameter(
      description = "The reference data domain required. This is case insensitive.",
      schema =
      Schema(
        type = "string",
        allowableValues = [
          "absence-reason",
          "absence-reason-category",
          "absence-sub-type",
          "absence-type",
        ],
      ),
    )
    @PathVariable domain: String,
    @PathVariable code: String,
  ): ResponseEntity<AbsenceCategorisations> = acRetriever.findOptions(ReferenceDataDomain.Code.of(domain), code)?.let {
    ResponseEntity.ok(it)
  } ?: ResponseEntity.noContent().build()

  @GetMapping("/filters")
  @PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
  fun getAbsenceCategorisationFilters(): AbsenceCategorisationFilters = acRetriever.getAbsenceCategorisationFilters()
}
