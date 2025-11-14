package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.Action
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import java.util.UUID

@Service
class TapOccurrenceModifications(
  private val taoRepository: TemporaryAbsenceOccurrenceRepository,
  private val referenceDataRepository: ReferenceDataRepository,
) {
  @Transactional
  fun apply(id: UUID, action: Action) {
    ExternalMovementContext.get().copy(reason = action.reason).set()
    val occurrence = taoRepository.getOccurrence(id)
    when (action) {
      is RescheduleOccurrence -> occurrence.reschedule(action)
      is CancelOccurrence -> {
        if (occurrence.status.code !in listOf(TapOccurrenceStatus.Code.SCHEDULED.name, TapOccurrenceStatus.Code.CANCELLED.name)) {
          throw ConflictException("Temporary absence not currently scheduled")
        } else {
          occurrence.cancel(action) { domain, code -> referenceDataRepository.getByKey(domain of code) }
        }
      }
    }
  }
}
