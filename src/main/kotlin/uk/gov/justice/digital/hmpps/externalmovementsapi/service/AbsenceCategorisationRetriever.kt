package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.CategorisedAbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.RdWithDomainLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomainRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByDomainAndCodeWithDomainLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.AbsenceCategorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription

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

  fun getReasonCategorisation(action: AbsenceCategorisationAction): CategorisedAbsenceReason = with(action) {
    val rdProvider = referenceDataRepository.rdProvider(this)
    val linkProvider = { nextDomain: ReferenceDataDomain.Code, previous: ReferenceData ->
      referenceDataRepository.findLinkedItems(nextDomain, previous.id).let {
        when (it.size) {
          0 -> null
          1 -> it.single()
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val type = requireNotNull(absenceTypeCode?.let { rdProvider(ABSENCE_TYPE, it) as AbsenceType })
    val subType = (
      absenceSubTypeCode?.let { rdProvider(ABSENCE_SUB_TYPE, it) }
        ?: linkProvider(ABSENCE_SUB_TYPE, type)
      ) as? AbsenceSubType
    val reasonCategory =
      (absenceReasonCategoryCode?.let { rdProvider(ABSENCE_REASON_CATEGORY, it) }) as? AbsenceReasonCategory
    val reason = (
      absenceReasonCode?.let { rdProvider(ABSENCE_REASON, it) }
        ?: linkProvider(ABSENCE_REASON, reasonCategory ?: subType ?: type)
      ) as? AbsenceReason

    return object : CategorisedAbsenceReason {
      override val absenceType: AbsenceType = type
      override val absenceSubType: AbsenceSubType? = subType
      override val absenceReasonCategory: AbsenceReasonCategory? = reasonCategory
      override val absenceReason: AbsenceReason? = reason
    }
  }
}

fun RdWithDomainLink.asAbsenceCategorisation() = with(referenceData) {
  AbsenceCategorisation(code, description, hintText, nextDomain)
}

fun ReferenceDataDomain.asCodedDescription() = CodedDescription(code.name, description)
