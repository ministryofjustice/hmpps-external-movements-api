package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AmendAuthorisationNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.AmendOccurrenceNotes
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
      val single = occurrence.authorisation.takeIf { !it.repeat }
      val readVersion = occurrence.version
      when (action) {
        is RescheduleOccurrence -> {
          single?.also {
            val cadr = ChangeAuthorisationDateRange(
              action.releaseAt?.toLocalDate() ?: it.fromDate,
              action.returnBy?.toLocalDate() ?: it.toDate,
              action.reason,
            )
            it.amendDateRange(cadr)
          } ?: occurrence.validateDateRange(action)
          occurrence.reschedule(action)
        }

        is CancelOccurrence -> {
          if (occurrence.status.code !in listOf(SCHEDULED.name, CANCELLED.name)) {
            throw ConflictException("Temporary absence not currently scheduled")
          } else {
            single?.cancel(CancelAuthorisation(action.reason)) { domain, code ->
              referenceDataRepository.getByKey(domain of code)
            }
            occurrence.cancel(action) { domain, code -> referenceDataRepository.getByKey(domain of code) }
          }
        }

        is AmendOccurrenceNotes -> {
          single?.amendNotes(AmendAuthorisationNotes(action.notes, action.reason))
          occurrence.amendNotes(action)
        }

        else -> throw IllegalArgumentException("Action not supported")
      }
      taoRepository.flush()
      readVersion!! to occurrence.version!!
    }!!
    return AuditHistory(listOfNotNull(occurrenceHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun TemporaryAbsenceOccurrence.validateDateRange(action: RescheduleOccurrence) {
    check(
      action.releaseAt?.toLocalDate()?.isBefore(authorisation.fromDate) != true &&
        action.returnBy?.toLocalDate()?.isAfter(authorisation.toDate) != true,
    ) {
      "Temporary absence must be within the authorised date range."
    }
  }
}
