package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.MovementInOutCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonAuthorisationCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonMovementsCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonOccurrenceCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonTapCounts

@Service
class CountPersonTemporaryAbsences(
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun temporaryAbsences(personIdentifier: String): PersonTapCounts {
    val authCount = authorisationRepository.countByPersonIdentifier(personIdentifier)
    val occCount = occurrenceRepository.countByAuthorisationPersonIdentifier(personIdentifier)
    val movementSummary = movementRepository.summaryForPerson(personIdentifier)
    return PersonTapCounts(
      PersonAuthorisationCount(authCount),
      PersonOccurrenceCount(occCount),
      PersonMovementsCount(
        MovementInOutCount(movementSummary.schOut, movementSummary.schIn),
        MovementInOutCount(movementSummary.adocOut, movementSummary.adhocIn),
      ),
    )
  }
}
