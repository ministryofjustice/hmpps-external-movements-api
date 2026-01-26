package uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.FilterPosition.Companion.positionOf
import java.util.UUID

data class AbsenceCategorisationFilters(
  val types: List<Option>,
  val subTypes: List<Option>,
  val reasons: List<Option>,
  val workTypes: List<Option>,
) {
  data class Option(val domainCode: ReferenceDataDomain.Code, val code: String, val description: String)

  companion object {
    fun from(
      referenceData: List<ReferenceData>,
      acLinks: List<AbsenceCategorisationLink>,
    ): AbsenceCategorisationFilters {
      val rd = referenceData.filter { it.active }.associateBy { it.id }
      val linkFrom = acLinks.groupBy { it.id1 }
      val types = rd.values.filterIsInstance<AbsenceType>().hierarchy(
        { linkFrom.getOrDefault(it, emptyList()) },
        { requireNotNull(rd[it]) },
      )
      val subTypes = types.filterIsInstance<Hierarchy.Node>().flatMap { it.linkedTo }
        .filter { it.position == FilterPosition.SUB_TYPE }
      val reasons = (types + subTypes).filterIsInstance<Hierarchy.Node>().flatMap { it.linkedTo }
        .filter { it.position == FilterPosition.REASON }
      val workTypes = reasons.filterIsInstance<Hierarchy.Node>().flatMap { it.linkedTo }
      return AbsenceCategorisationFilters(
        types.map(Hierarchy::asOption).sorted(),
        subTypes.map(Hierarchy::asOption).sorted(),
        reasons.map(Hierarchy::asOption).sorted(),
        workTypes.map(Hierarchy::asOption).sorted(),
      )
    }

    private fun List<ReferenceData>.hierarchy(
      linkFrom: (UUID) -> List<AbsenceCategorisationLink>,
      linked: (UUID) -> ReferenceData,
    ): List<Hierarchy> = map {
      Hierarchy.of(ReferenceDataDomain.Code.ABSENCE_TYPE, it, linkFrom, linked, null)
    }
  }
}

enum class FilterPosition {
  TYPE,
  SUB_TYPE,
  REASON,
  WORK_TYPE,
  ;

  companion object {
    fun positionOf(rd: ReferenceData): FilterPosition = when (rd) {
      is AbsenceType -> TYPE
      is AbsenceSubType -> SUB_TYPE
      else -> REASON
    }
  }
}

sealed interface Hierarchy {
  val domainCode: ReferenceDataDomain.Code
  val referenceData: ReferenceData
  val parentPrefix: String?
  val position: FilterPosition

  data class Node(
    override val domainCode: ReferenceDataDomain.Code,
    override val referenceData: ReferenceData,
    val linkedTo: List<Hierarchy>,
    override val parentPrefix: String?,
    override val position: FilterPosition,
  ) : Hierarchy

  data class Leaf(
    override val domainCode: ReferenceDataDomain.Code,
    override val referenceData: ReferenceData,
    override val parentPrefix: String?,
    override val position: FilterPosition,
  ) : Hierarchy

  companion object {
    fun of(
      domainCode: ReferenceDataDomain.Code,
      head: ReferenceData,
      linkFrom: (UUID) -> List<AbsenceCategorisationLink>,
      linked: (UUID) -> ReferenceData,
      prefixOverride: String?,
      positionOverride: FilterPosition? = null,
    ): Hierarchy {
      val links = linkFrom(head.id).map {
        of(it.domain2, linked(it.id2), linkFrom, linked, head.parentPrefix())
      }
      return if (head is DomainLinkedReferenceData && head.nextDomain != null && links.size > 1) {
        Node(domainCode, head, links, prefixOverride, positionOverride ?: positionOf(head))
      } else {
        links.singleOrNull()?.let {
          of(
            it.domainCode,
            it.referenceData,
            linkFrom,
            linked,
            head.parentPrefix(),
            positionOverride ?: positionOf(head),
          )
        } ?: Leaf(domainCode, head, prefixOverride, positionOverride ?: positionOf(head))
      }
    }
  }
}

private fun ReferenceData.parentPrefix(): String? = when (this) {
  is AbsenceReasonCategory -> {
    when (code) {
      "PW" -> "Paid work"
      "UW" -> "Unpaid work"
      else -> null
    }
  }

  else -> {
    null
  }
}

private fun ReferenceData.isYouth() = this !is AbsenceType && code.startsWith("Y")

private fun Hierarchy.asOption(): AbsenceCategorisationFilters.Option = AbsenceCategorisationFilters.Option(
  domainCode,
  referenceData.code,
  parentPrefix?.let { "$parentPrefix - ${referenceData.description}" }
    ?: if (referenceData.isYouth()) {
      "Youth - ${referenceData.description}"
    } else {
      referenceData.description
    },
)

private fun List<AbsenceCategorisationFilters.Option>.sorted() = distinct().sortedBy { it.description.lowercase() }
