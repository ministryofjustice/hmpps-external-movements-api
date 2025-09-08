package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.RdWithDomainLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomainRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CodedDescription

@Service
class AbsenceCategorisationRetriever(
  private val domainRepository: ReferenceDataDomainRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun findByDomain(domain: ReferenceDataDomain.Type): AbsenceCategorisations = domainRepository.findByIdOrNull(domain)
    ?.let { rdd ->
      referenceDataRepository.findWithDomainLink(rdd.code)
        .sortedBy { it.referenceData.sequenceNumber }
        .map(RdWithDomainLink::asAbsenceCategorisation)
        .let { AbsenceCategorisations(rdd.asCodedDescription(), it) }
    } ?: throw NotFoundException("Reference data domain not found")
}

fun RdWithDomainLink.asAbsenceCategorisation() = with(referenceData) {
  AbsenceCategorisation(code, description, hintText, nextDomain)
}

fun ReferenceDataDomain.asCodedDescription() = CodedDescription(code.name, description)
