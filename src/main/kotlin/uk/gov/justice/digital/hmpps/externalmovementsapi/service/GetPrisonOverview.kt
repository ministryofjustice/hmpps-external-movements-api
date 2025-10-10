package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.PrisonLeaverCounts
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview

@Service
class GetPrisonOverview(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
) {
  fun forPrison(prisonIdentifier: String): PrisonExternalMovementOverview {
    val leaversCounts = occurrenceRepository.findUpcomingLeaverCounts(prisonIdentifier) ?: PrisonLeaverCounts.Default
    val returningToday = occurrenceRepository.findReturningTodayCount(prisonIdentifier)
    val approvalsRequired = authorisationRepository.findApprovalsRequiredCount(prisonIdentifier)

    return PrisonExternalMovementOverview(
      PrisonExternalMovementOverview.Configuration.DEFAULT,
      leaversCounts.asTapOverview(returningToday, approvalsRequired),
    )
  }
}

private fun PrisonLeaverCounts.asTapOverview(returningToday: Int, approvalsRequired: Int) = PrisonExternalMovementOverview.TapOverview(leavingToday, returningToday, leavingNextSevenDays, approvalsRequired)
