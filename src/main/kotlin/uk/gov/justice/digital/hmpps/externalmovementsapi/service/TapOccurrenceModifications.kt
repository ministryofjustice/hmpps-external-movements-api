package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AmendAuthorisationNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.AmendOccurrenceNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
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
      val rdSupplier =
        { domain: ReferenceDataDomain.Code, code: String -> referenceDataRepository.getByKey(domain of code) }
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
            single?.cancel(CancelAuthorisation(action.reason), rdSupplier)
            occurrence.cancel(action, rdSupplier)
          }
        }

        is AmendOccurrenceNotes -> {
          single?.amendNotes(AmendAuthorisationNotes(action.notes, action.reason))
          occurrence.amendNotes(action)
        }

        is ChangeOccurrenceAccompaniment -> {
          single?.applyAccompaniment(
            ChangeAuthorisationAccompaniment(action.accompaniedByCode, action.reason),
            rdSupplier,
          )
          occurrence.applyAccompaniment(action, rdSupplier)
        }

        is ChangeOccurrenceTransport -> {
          single?.applyTransport(ChangeAuthorisationTransport(action.transportCode, action.reason), rdSupplier)
          occurrence.applyTransport(action, rdSupplier)
        }

        is RecategoriseOccurrence -> {
          val (action, rdSupplier) = action.recalculateCategorisation()
          single?.applyAbsenceCategorisation(RecategoriseAuthorisation(action), rdSupplier)
          occurrence.applyAbsenceCategorisation(action, rdSupplier)
        }

        is ChangeOccurrenceLocation -> {
          occurrence.applyLocation(action)
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

  private fun RecategoriseOccurrence.recalculateCategorisation(): Pair<RecategoriseOccurrence, (ReferenceDataDomain.Code, String) -> ReferenceData> {
    val rdProvider = referenceDataRepository.rdProvider(this)
    val linkProvider = { nextDomain: ReferenceDataDomain.Code, previous: ReferenceData ->
      referenceDataRepository.findLinkedItems(nextDomain, previous.id).let {
        when (it.size) {
          0 -> null
          1 -> it.single()
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val type = requireNotNull(absenceTypeCode?.let { rdProvider(ABSENCE_TYPE, it) as AbsenceType })
    val subType = (absenceSubTypeCode?.let { rdProvider(ABSENCE_SUB_TYPE, it) } ?: linkProvider(ABSENCE_SUB_TYPE, type)) as? AbsenceSubType
    val reasonCategory = (absenceReasonCategoryCode?.let { rdProvider(ABSENCE_REASON_CATEGORY, it) }) as? AbsenceReasonCategory
    val reason = (absenceReasonCode?.let { rdProvider(ABSENCE_REASON, it) } ?: linkProvider(ABSENCE_REASON, reasonCategory ?: subType ?: type)) as? AbsenceReason
    val newAction = copy(
      absenceTypeCode = type.code,
      absenceSubTypeCode = subType?.code,
      absenceReasonCategoryCode = reasonCategory?.code,
      absenceReasonCode = reason?.code,
    )
    return newAction to referenceDataRepository.rdProvider(newAction)
  }
}
