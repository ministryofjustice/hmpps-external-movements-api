package uk.gov.justice.digital.hmpps.externalmovementsapi.em.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.em.model.PrisonExternalMovementOverview
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository

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
