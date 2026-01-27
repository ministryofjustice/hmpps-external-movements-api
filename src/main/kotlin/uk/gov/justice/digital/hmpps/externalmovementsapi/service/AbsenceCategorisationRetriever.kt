package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomainRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.CategorisedAbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Hintable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.AbsenceCategorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisationFilters
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import kotlin.reflect.KClass

@Service
class AbsenceCategorisationRetriever(
  private val domainRepository: ReferenceDataDomainRepository,
  private val acLinkRepository: AbsenceCategorisationLinkRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun findByDomain(code: ReferenceDataDomain.Code): AbsenceCategorisations {
    val domain = domainRepository.getDomain(code)
    return referenceDataRepository.findAllByType(code.clazz)
      .filter { it.active }
      .sortedBy { it.sequenceNumber }
      .map(ReferenceData::asAbsenceCategorisation)
      .let { AbsenceCategorisations(domain.asCodedDescription(), it) }
  }

  fun findOptions(domainCode: ReferenceDataDomain.Code, rdCode: String): AbsenceCategorisations? {
    val linked: DomainLinkedReferenceData? = referenceDataRepository.findAllByType(domainCode.clazz)
      .firstOrNull { it.code == rdCode }.takeIf { it is DomainLinkedReferenceData } as? DomainLinkedReferenceData
    return linked?.nextDomain?.let { dc ->
      val domain = domainRepository.getDomain(dc)
      val items = referenceDataRepository.findAllByType(dc.clazz).associateBy { it.id }
      val links = acLinkRepository.findById1AndDomain2(linked.id, dc)
        .sortedBy { it.sequenceNumber }
        .mapNotNull { items[it.id2]?.asAbsenceCategorisation() }
      AbsenceCategorisations(domain.asCodedDescription(), links)
    }
  }

  fun getReasonCategorisation(action: AbsenceCategorisationAction) = with(action) {
    val allRd = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val rdProvider = { clazz: KClass<out ReferenceData>, code: String -> allRd[clazz to code] }
    val linkProvider = { nextDomain: ReferenceDataDomain.Code, previous: ReferenceData ->
      acLinkRepository.findById1AndDomain2(previous.id, nextDomain).let {
        when (it.size) {
          0 -> null
          1 -> it.single().let { link -> allRd.values.first { rd -> rd.id == link.id2 } }
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val type = requireNotNull(absenceTypeCode?.let { rdProvider(AbsenceType::class, it) as AbsenceType })
    val subType =
      absenceSubTypeCode?.let { rdProvider(AbsenceSubType::class, it) } ?: linkProvider(ABSENCE_SUB_TYPE, type)
    val reasonCategory = absenceReasonCategoryCode?.let { rdProvider(AbsenceReasonCategory::class, it) }
    val reason = absenceReasonCode?.let { rdProvider(AbsenceReason::class, it) }
      ?: linkProvider(ABSENCE_REASON, reasonCategory ?: subType ?: type)

    val car = object : CategorisedAbsenceReason {
      override val absenceType: AbsenceType = type
      override val absenceSubType: AbsenceSubType? = subType as? AbsenceSubType
      override val absenceReasonCategory: AbsenceReasonCategory? = reasonCategory as? AbsenceReasonCategory
      override val absenceReason: AbsenceReason = reason as AbsenceReason
    }
    car to allRd
  }

  fun getAbsenceCategorisationFilters(): AbsenceCategorisationFilters = AbsenceCategorisationFilters.from(referenceDataRepository.findAll(), acLinkRepository.findAll())
}

fun ReferenceDataDomain.asCodedDescription() = CodedDescription(code.name, description)

fun ReferenceData.asAbsenceCategorisation() = AbsenceCategorisation(
  code,
  description,
  if (this is Hintable) hintText else null,
  if (this is DomainLinkedReferenceData) nextDomain else null,
)
