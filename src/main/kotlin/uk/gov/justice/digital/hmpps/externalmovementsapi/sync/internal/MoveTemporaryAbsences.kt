package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.MoveTemporaryAbsencesRequest

@Transactional
@Service
class MoveTemporaryAbsences(
  private val personSummaryService: PersonSummaryService,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun move(request: MoveTemporaryAbsencesRequest) {
    val moveTo = personSummaryService.getWithSave(request.toPersonIdentifier)
    occurrenceRepository.findByAuthorisationIdIn(request.authorisationIds).forEach { tao ->
      check(request contains tao.authorisation.person.identifier) { EXCEPTION_MESSAGE }
      tao.authorisation.moveTo(moveTo)
      tao.movements().forEach {
        check(request contains it.person.identifier) { EXCEPTION_MESSAGE }
        it.moveTo(moveTo)
      }
    }
    movementRepository.findAllById(request.unscheduledMovementIds).forEach {
      it.moveTo(moveTo)
    }
  }

  private infix fun MoveTemporaryAbsencesRequest.contains(personIdentifier: String): Boolean = fromPersonIdentifier == personIdentifier || toPersonIdentifier == personIdentifier

  companion object {
    const val EXCEPTION_MESSAGE = "Assigned to the wrong person and cannot be moved"
  }
}
