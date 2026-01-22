package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.PrisonExternalMovementOverview

@Service
class GetPrisonOverview(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
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
}
