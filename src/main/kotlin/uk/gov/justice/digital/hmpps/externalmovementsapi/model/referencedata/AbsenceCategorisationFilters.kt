package uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.DomainLinkedReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLink
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
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
        .filter { it.domainCode == ReferenceDataDomain.Code.ABSENCE_SUB_TYPE }
      val categories = subTypes.filterIsInstance<Hierarchy.Node>().flatMap { it.linkedTo }
      val reasons = (types + subTypes).filterIsInstance<Hierarchy.Node>().flatMap { it.linkedTo }
        .filter { it.domainCode != ReferenceDataDomain.Code.ABSENCE_SUB_TYPE }
      val workTypes = categories.filterIsInstance<Hierarchy.Node>().flatMap { it.linkedTo }
      return AbsenceCategorisationFilters(
        types.map(Hierarchy::asOption).sorted(),
        subTypes.map(Hierarchy::asOption).sorted(),
        (categories + reasons).map(Hierarchy::asOption).sorted(),
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

sealed interface Hierarchy {
  val domainCode: ReferenceDataDomain.Code
  val referenceData: ReferenceData
  val parentPrefix: String?

  data class Node(
    override val domainCode: ReferenceDataDomain.Code,
    override val referenceData: ReferenceData,
    val linkedTo: List<Hierarchy>,
    override val parentPrefix: String?,
  ) : Hierarchy

  data class Leaf(
    override val domainCode: ReferenceDataDomain.Code,
    override val referenceData: ReferenceData,
    override val parentPrefix: String?,
  ) : Hierarchy

  companion object {
    fun of(
      domainCode: ReferenceDataDomain.Code,
      head: ReferenceData,
      linkFrom: (UUID) -> List<AbsenceCategorisationLink>,
      linked: (UUID) -> ReferenceData,
      parentPrefix: String?,
    ): Hierarchy {
      val links = linkFrom(head.id).map {
        of(it.domain2, linked(it.id2), linkFrom, linked, head.workTypePrefix())
      }
      return if (head is DomainLinkedReferenceData && head.nextDomain != null && links.size > 1) {
        Node(domainCode, head, links, parentPrefix)
      } else {
        links.singleOrNull()?.let {
          of(it.domainCode, it.referenceData, linkFrom, linked, head.workTypePrefix())
        } ?: Leaf(domainCode, head, parentPrefix)
      }
    }
  }
}

private fun ReferenceData.workTypePrefix(): String? = if (this is AbsenceReasonCategory) {
  when (code) {
    "PW" -> "Paid work"
    "UW" -> "Unpaid work"
    else -> null
  }
} else {
  null
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
