package uk.gov.justice.digital.hmpps.externalmovementsapi.domain

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey

data class ReasonPath(val path: List<ReferenceDataKey>) {
  fun has(domain: ReferenceDataDomain.Code): Boolean = path.any { it.domain == domain }
}
