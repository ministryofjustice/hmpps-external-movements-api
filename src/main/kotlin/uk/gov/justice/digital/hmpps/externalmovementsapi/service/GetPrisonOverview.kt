package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.PrisonAbsenceCategorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.PrisonAbsenceCategorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.PrisonAbsenceCategorisations

@Service
class GetPrisonOverview(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val pacRepository: PrisonAbsenceCategorisationRepository,
) {
  fun forPrison(prisonIdentifier: String): PrisonExternalMovementOverview {
    val leavingToday = occurrenceRepository.findLeavingTodayCount(prisonIdentifier)
    val returningToday = occurrenceRepository.findReturningTodayCount(prisonIdentifier)
    val approvalsRequired = authorisationRepository.findApprovalsRequiredCount(prisonIdentifier)

    return PrisonExternalMovementOverview(
      PrisonExternalMovementOverview.Configuration.DEFAULT,
      PrisonExternalMovementOverview.TapOverview(leavingToday, returningToday, approvalsRequired),
    )
  }

  fun findCategorisationTypesForPrison(prisonIdentifier: String): PrisonAbsenceCategorisations {
    val groups = pacRepository.findByPrisonCode(prisonIdentifier)
      .groupBy { it.domainCode }
      .map { e ->
        e.key to e.value.map { CodedDescription(it.code, it.description()) }.sortedBy { it.description }
      }.toMap()
    return PrisonAbsenceCategorisations(
      groups.getOrDefault(ABSENCE_TYPE, emptyList()),
      groups.getOrDefault(ABSENCE_SUB_TYPE, emptyList()),
      groups.getOrDefault(ABSENCE_REASON_CATEGORY, emptyList()),
      groups.getOrDefault(ABSENCE_REASON, emptyList()),
    )
  }

  private fun PrisonAbsenceCategorisation.description() = if (isYouthRdr()) {
    "Youth - $description"
  } else {
    description
  }

  private fun PrisonAbsenceCategorisation.isYouthRdr(): Boolean = domainCode == ABSENCE_REASON && code == "YRDR"
}
