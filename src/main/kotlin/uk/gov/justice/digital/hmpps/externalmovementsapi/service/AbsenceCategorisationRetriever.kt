package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.RdWithDomainLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomainRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.getByDomainAndCodeWithDomainLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.getDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CodedDescription

@Service
class AbsenceCategorisationRetriever(
  private val domainRepository: ReferenceDataDomainRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val rdLinkRepository: ReferenceDataLinkRepository,
) {
  fun findByDomain(code: ReferenceDataDomain.Code): AbsenceCategorisations {
    val domain = domainRepository.getDomain(code)
    return referenceDataRepository.findWithDomainLink(domain.code)
      .sortedBy { it.referenceData.sequenceNumber }
      .map(RdWithDomainLink::asAbsenceCategorisation)
      .let { AbsenceCategorisations(domain.asCodedDescription(), it) }
  }

  fun findOptions(domainCode: ReferenceDataDomain.Code, rdCode: String): AbsenceCategorisations? {
    val rdWithLink = referenceDataRepository.getByDomainAndCodeWithDomainLink(domainCode, rdCode)
    return rdWithLink.nextDomain?.let { dc ->
      val domain = domainRepository.getDomain(dc)
      val links = rdLinkRepository.findLinksByIdAndNextDomain(rdWithLink.referenceData.id, dc)
        .sortedBy { it.sequenceNumber }
        .map { it.asAbsenceCategorisation() }
      AbsenceCategorisations(domain.asCodedDescription(), links)
    }
  }
}

fun RdWithDomainLink.asAbsenceCategorisation() = with(referenceData) {
  AbsenceCategorisation(code, description, hintText, nextDomain)
}

fun ReferenceDataDomain.asCodedDescription() = CodedDescription(code.name, description)
