package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AbsenceCategorisation

@Service
class AbsenceCategorisationRetriever(private val referenceDataRepository: ReferenceDataRepository) {
  fun findByDomain(domain: ReferenceDataDomain): List<AbsenceCategorisation> = referenceDataRepository.findByKeyDomain(domain).filter { it.active }
    .sortedBy(ReferenceData::sequenceNumber)
    .map(ReferenceData::asAbsenceCategorisation)
}

fun ReferenceData.asAbsenceCategorisation() = AbsenceCategorisation(code, description, hintText, null)
