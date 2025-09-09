package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.ReferenceDataResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.ReferenceDataService

@RestController
@RequestMapping("reference-data")
class ReferenceDataController(private val rdService: ReferenceDataService) {
  @PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
  @GetMapping("/{domain}")
  fun getDomain(@PathVariable domain: String): ReferenceDataResponse = rdService.findByDomain(ReferenceDataDomain.Code.of(domain))
}
