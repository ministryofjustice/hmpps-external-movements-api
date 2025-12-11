package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON

class ReferenceDataPaths(data: List<RdWithDomainLink>, private val parentOf: (id: Long) -> List<ReferenceData>) {
  private val data = data.associateBy { it.referenceData.key }.toMutableMap()
  fun getReferenceData(domain: ReferenceDataDomain.Code, code: String): ReferenceData = requireNotNull(data[domain of code]).referenceData
  fun findReferenceData(domain: ReferenceDataDomain.Code, code: String): ReferenceData? = data[domain of code]?.referenceData

  fun reasonPath(): ReasonPath {
    val type = data.values.singleOrNull { it.domain == ReferenceDataDomain.Code.ABSENCE_TYPE }
    val subType = type?.takeIf { it.nextDomain == ReferenceDataDomain.Code.ABSENCE_SUB_TYPE }?.let {
      data.values.singleOrNull { it.domain == ReferenceDataDomain.Code.ABSENCE_SUB_TYPE }
    }
    val reason = data.values.singleOrNull { it.domain == ABSENCE_REASON }
    val category = subType?.takeIf { it.nextDomain == ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY }?.let {
      data.values.singleOrNull { it.domain == ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY }
        ?: reason?.referenceData?.id?.let { reasonId -> parentOf(reasonId) }?.let { category ->
          when (category.size) {
            1 -> object : RdWithDomainLink {
              override val referenceData: ReferenceData = category.single()
              override val nextDomain: ReferenceDataDomain.Code = ABSENCE_REASON
            }.also {
              data[it.referenceData.key] = it
            }

            else -> null
          }
        }
    }
    return ReasonPath(
      buildList {
        type?.referenceData?.key?.also { add(it) }
        subType?.referenceData?.key?.also { add(it) }
        category?.referenceData?.key?.also { add(it) }
        if (ABSENCE_REASON in listOfNotNull(type?.nextDomain, subType?.nextDomain, category?.nextDomain) ||
          // Sync sometimes only provides a reason
          listOfNotNull(type, subType, category).isEmpty()
        ) {
          reason?.referenceData?.key?.also { add(it) }
        }
      },
    )
  }
}
