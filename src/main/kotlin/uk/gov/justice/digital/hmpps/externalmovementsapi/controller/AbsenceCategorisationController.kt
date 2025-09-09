package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.AbsenceCategorisationRetriever

@RestController
@RequestMapping("absence-categorisation")
class AbsenceCategorisationController(private val acRetriever: AbsenceCategorisationRetriever) {
  @PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
  @GetMapping("/{domain}")
  fun getDomain(@PathVariable domain: String): AbsenceCategorisations = acRetriever.findByDomain(ReferenceDataDomain.Code.of(domain))

  @PreAuthorize("hasRole('${Roles.EXTERNAL_MOVEMENTS_UI}')")
  @GetMapping("/{domain}/{code}")
  fun getOptions(@PathVariable domain: String, @PathVariable code: String): ResponseEntity<AbsenceCategorisations> = acRetriever.findOptions(ReferenceDataDomain.Code.of(domain), code)?.let {
    ResponseEntity.ok(it)
  } ?: ResponseEntity.noContent().build()
}
