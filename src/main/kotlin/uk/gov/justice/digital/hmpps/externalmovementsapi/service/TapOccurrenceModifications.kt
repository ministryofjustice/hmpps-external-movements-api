package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.history.OccurrenceHistory
import java.util.UUID

@Service
class TapOccurrenceModifications(
  private val transactionTemplate: TransactionTemplate,
  private val taoRepository: TemporaryAbsenceOccurrenceRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val occurrenceHistory: OccurrenceHistory,
) {
  fun apply(id: UUID, action: OccurrenceAction): AuditHistory {
    ExternalMovementContext.get().copy(reason = action.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val occurrence = taoRepository.getOccurrence(id)
      val readVersion = occurrence.version
      when (action) {
        is RescheduleOccurrence -> occurrence.reschedule(action)
        is CancelOccurrence -> {
          if (occurrence.status.code !in listOf(SCHEDULED.name, CANCELLED.name)) {
            throw ConflictException("Temporary absence not currently scheduled")
          } else {
            occurrence.cancel(action) { domain, code -> referenceDataRepository.getByKey(domain of code) }
          }
        }

        else -> throw IllegalArgumentException("Action not supported")
      }
      taoRepository.flush()
      readVersion!! to occurrence.version!!
    }!!
    return AuditHistory(listOfNotNull(occurrenceHistory.currentAction(id, readVersion, writeVersion)))
  }
}
