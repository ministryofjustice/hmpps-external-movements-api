package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import java.util.UUID
import kotlin.reflect.KClass

class ReferenceDataPaths(
  data: List<ReferenceData>,
  private val parentOf: (id: UUID, domainCode: ReferenceDataDomain.Code) -> List<ReferenceData>,
) {
  private val data = data.associateBy { it::class to it.code }.toMutableMap()
  fun getReferenceData(domain: KClass<out ReferenceData>, code: String): ReferenceData = requireNotNull(data[domain to code])

  fun findReferenceData(domain: KClass<out ReferenceData>, code: String): ReferenceData? = data[domain to code]

  fun reasonPath(): ReasonPath {
    val type = data.values.singleOrNull { it is AbsenceType }
    val subType =
      type?.takeIf { it is DomainLinkedReferenceData && it.nextDomain == ReferenceDataDomain.Code.ABSENCE_SUB_TYPE }
        ?.let {
          data.values.singleOrNull { it is AbsenceSubType }
        }
    val reason = data.values.singleOrNull { it is AbsenceReason }
    val category = data.values.singleOrNull { it is AbsenceReasonCategory } ?: reason?.id?.let { reasonId ->
      parentOf(
        reasonId,
        ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY,
      )
    }?.let { category ->
      when (category.size) {
        1 -> category.single().also {
          data[it::class to it.code] = it
        }

        else -> null
      }
    }
    val hierarchy = listOfNotNull(type, subType, category)
    val reasonIsNextDomain: Boolean = ReferenceDataDomain.Code.ABSENCE_REASON in
      hierarchy.filterIsInstance<DomainLinkedReferenceData>().map { it.nextDomain }
    return ReasonPath(
      buildList {
        type?.code?.also { add(ReferenceDataDomain.Code.ABSENCE_TYPE of it) }
        subType?.code?.also { add(ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of it) }
        category?.code?.also { add(ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of it) }
        if (reasonIsNextDomain || hierarchy.isEmpty()) {
          reason?.code?.also { add(ReferenceDataDomain.Code.ABSENCE_REASON of it) }
        }
      },
    )
  }
}
