package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.ReferenceDataService

@RestController
@RequestMapping("reference-data")
@PreAuthorize("hasAnyRole('${Roles.TEMPORARY_ABSENCE_RO}', '${Roles.TEMPORARY_ABSENCE_RW}')")
class ReferenceDataController(private val rdService: ReferenceDataService) {
  @GetMapping("/{domain}")
  fun getDomain(
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
          "accompanied-by",
          "tap-authorisation-status",
          "tap-occurrence-status",
          "transport",
        ],
      ),
    )
    @PathVariable domain: String,
  ): ReferenceDataResponse = rdService.findByDomain(ReferenceDataDomain.Code.of(domain))
}
