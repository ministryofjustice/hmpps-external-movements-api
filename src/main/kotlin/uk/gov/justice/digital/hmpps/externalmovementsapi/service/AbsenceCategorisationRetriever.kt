package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import jakarta.transaction.NotSupportedException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomainRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.CategorisedAbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.FindByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Hintable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategoryRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubTypeRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceTypeRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.AbsenceCategorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.AbsenceCategorisations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.util.UUID
import kotlin.reflect.KClass

@Service
class AbsenceCategorisationRetriever(
  private val domainRepository: ReferenceDataDomainRepository,
  private val absenceTypeRepository: AbsenceTypeRepository,
  private val absenceSubTypeRepository: AbsenceSubTypeRepository,
  private val absenceReasonCategoryRepository: AbsenceReasonCategoryRepository,
  private val absenceReasonRepository: AbsenceReasonRepository,
  private val absenceCategorisationLinkRepository: AbsenceCategorisationLinkRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun findByDomain(code: ReferenceDataDomain.Code): AbsenceCategorisations {
    val domain = domainRepository.getDomain(code)
    return getDomain(code).findAll()
      .filter { it.active }
      .sortedBy { it.sequenceNumber }
      .map(ReferenceData::asAbsenceCategorisation)
      .let { AbsenceCategorisations(domain.asCodedDescription(), it) }
  }

  fun findOptions(domainCode: ReferenceDataDomain.Code, rdCode: String): AbsenceCategorisations? {
    val linked = getDomainItem(domainCode)?.findByCode(rdCode)
    return linked?.nextDomain?.let { dc ->
      val domain = domainRepository.getDomain(dc)
      val items = getDomain(dc).findAll().associateBy { it.id }
      val links = absenceCategorisationLinkRepository.findById1AndDomain2(linked.id, dc)
        .sortedBy { it.sequenceNumber }
        .mapNotNull { items[it.id2]?.asAbsenceCategorisation() }
      AbsenceCategorisations(domain.asCodedDescription(), links)
    }
  }

  fun getReasonCategorisation(action: AbsenceCategorisationAction) = with(action) {
    val allRd = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val rdProvider = { clazz: KClass<out ReferenceData>, code: String -> allRd[clazz to code] }
    val linkProvider = { nextDomain: ReferenceDataDomain.Code, previous: ReferenceData ->
      absenceCategorisationLinkRepository.findById1AndDomain2(previous.id, nextDomain).let {
        when (it.size) {
          0 -> null
          1 -> it.single().let { link -> allRd.values.first { rd -> rd.id == link.id2 } }
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val type = requireNotNull(absenceTypeCode?.let { rdProvider(AbsenceType::class, it) as AbsenceType })
    val subType = (
      absenceSubTypeCode?.let { rdProvider(AbsenceSubType::class, it) }
        ?: linkProvider(ABSENCE_SUB_TYPE, type)
      ) as? AbsenceSubType
    val reasonCategory =
      (absenceReasonCategoryCode?.let { rdProvider(AbsenceReasonCategory::class, it) }) as? AbsenceReasonCategory
    val reason = (
      absenceReasonCode?.let { rdProvider(AbsenceReason::class, it) }
        ?: linkProvider(ABSENCE_REASON, reasonCategory ?: subType ?: type)
      ) as? AbsenceReason

    val car = object : CategorisedAbsenceReason {
      override val absenceType: AbsenceType = type
      override val absenceSubType: AbsenceSubType? = subType
      override val absenceReasonCategory: AbsenceReasonCategory? = reasonCategory
      override val absenceReason: AbsenceReason? = reason
    }
    car to allRd
  }

  private fun getDomain(domainCode: ReferenceDataDomain.Code): JpaRepository<out ReferenceData, UUID> = when (domainCode) {
    ABSENCE_REASON -> absenceReasonRepository
    ABSENCE_REASON_CATEGORY -> absenceReasonCategoryRepository
    ABSENCE_SUB_TYPE -> absenceSubTypeRepository
    ABSENCE_TYPE -> absenceTypeRepository
    else -> throw NotSupportedException("Only absence categorisation is supported for domain links")
  }

  private fun getDomainItem(domainCode: ReferenceDataDomain.Code): FindByCode<out DomainLinkedReferenceData>? = when (domainCode) {
    ABSENCE_REASON -> null
    ABSENCE_REASON_CATEGORY -> absenceReasonCategoryRepository
    ABSENCE_SUB_TYPE -> absenceSubTypeRepository
    ABSENCE_TYPE -> absenceTypeRepository
    else -> throw NotSupportedException("Only absence categorisation is supported for domain links")
  }
}

fun ReferenceDataDomain.asCodedDescription() = CodedDescription(code.name, description)

fun ReferenceData.asAbsenceCategorisation() = AbsenceCategorisation(
  code,
  description,
  if (this is Hintable) hintText else null,
  if (this is DomainLinkedReferenceData) nextDomain else null,
)
