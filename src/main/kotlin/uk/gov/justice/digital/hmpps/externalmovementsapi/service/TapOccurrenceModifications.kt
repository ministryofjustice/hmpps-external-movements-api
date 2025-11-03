package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.Action
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import java.util.UUID

@Service
class TapOccurrenceModifications(private val taoRepository: TemporaryAbsenceOccurrenceRepository) {
  @Transactional
  fun apply(id: UUID, action: Action) {
    val occurrence = taoRepository.getOccurrence(id)
    when (action) {
      is RescheduleOccurrence -> occurrence.reschedule(action)
    }
  }
}
