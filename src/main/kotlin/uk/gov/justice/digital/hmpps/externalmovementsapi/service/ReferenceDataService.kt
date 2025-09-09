package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomainRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.getDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.ReferenceDataResponse

@Service
class ReferenceDataService(
  private val domainRepository: ReferenceDataDomainRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun findByDomain(code: ReferenceDataDomain.Code): ReferenceDataResponse {
    val domain = domainRepository.getDomain(code)
    val items = referenceDataRepository.findByKeyDomainAndActiveTrue(code)
    return ReferenceDataResponse(domain.asCodedDescription(), items.map(ReferenceData::asCodedDescription))
  }
}
