package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.OccurrenceAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.OccurrenceActions
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.history.OccurrenceHistory
import java.util.UUID
import kotlin.reflect.KClass

@Service
class TapOccurrenceModifications(
  private val transactionTemplate: TransactionTemplate,
  private val taoRepository: TemporaryAbsenceOccurrenceRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val acr: AbsenceCategorisationRetriever,
  private val occurrenceHistory: OccurrenceHistory,
) {
  fun apply(id: UUID, action: OccurrenceAction): AuditHistory {
    ExternalMovementContext.get().copy(reason = action.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val occurrence = taoRepository.getOccurrence(id)
      val readVersion = occurrence.version
      val rdSupplier = referenceDataRepository.rdProvider()
      occurrence.applyAction(action, rdSupplier)
      taoRepository.flush()
      readVersion!! to occurrence.version!!
    }
    return AuditHistory(listOfNotNull(occurrenceHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun TemporaryAbsenceOccurrence.applyAction(
    action: OccurrenceAction,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    val single = authorisation.takeIf { !it.repeat }
    when (action) {
      is RescheduleOccurrence -> {
        single?.also {
          val cadr = ChangeAuthorisationDateRange(
            action.start?.toLocalDate() ?: it.start,
            action.end?.toLocalDate() ?: it.end,
          )
          it.applyDateRange(cadr, rdSupplier)
        } ?: validateDateRange(action)
        reschedule(action)
        calculateStatus { rdSupplier(OccurrenceStatus::class, it) as OccurrenceStatus }
      }

      is CancelOccurrence -> {
        if (status.code !in listOf(SCHEDULED.name, CANCELLED.name)) {
          throw ConflictException("Temporary absence not currently scheduled")
        } else {
          single?.cancel(CancelAuthorisation(), rdSupplier)?.also {
            makeDpsOnly()
          }
          cancel(action, rdSupplier)
        }
      }

      is ChangeOccurrenceComments -> {
        single?.applyComments(ChangeAuthorisationComments(action.comments))
        applyComments(action)
      }

      is ChangeOccurrenceAccompaniment -> {
        single?.applyAccompaniment(
          ChangeAuthorisationAccompaniment(action.accompaniedByCode),
          rdSupplier,
        )
        applyAccompaniment(action, rdSupplier)
      }

      is ChangeOccurrenceTransport -> {
        single?.applyTransport(ChangeAuthorisationTransport(action.transportCode), rdSupplier)
        applyTransport(action, rdSupplier)
      }

      is RecategoriseOccurrence -> {
        val (action, rdSupplier) = action.recalculateCategorisation()
        single?.applyAbsenceCategorisation(RecategoriseAuthorisation(action), rdSupplier)
        applyAbsenceCategorisation(action, rdSupplier)
      }

      is ChangeOccurrenceLocation -> {
        applyLocation(action)
        with(authorisation) {
          val newLocations = if (repeat) {
            (locations + location).mapTo(linkedSetOf()) { it }
          } else {
            linkedSetOf(location)
          }
          applyLocations(ChangeAuthorisationLocations(newLocations))
        }
      }

      else -> throw IllegalArgumentException("Action not supported")
    }
  }

  fun apply(id: UUID, request: OccurrenceActions): AuditHistory {
    ExternalMovementContext.get().copy(reason = request.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val occurrence = taoRepository.getOccurrence(id)
      val readVersion = occurrence.version
      val rdSupplier = referenceDataRepository.rdProvider()
      request.actions.forEach { occurrence.applyAction(it, rdSupplier) }
      taoRepository.flush()
      readVersion!! to occurrence.version!!
    }
    return AuditHistory(listOfNotNull(occurrenceHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun TemporaryAbsenceOccurrence.validateDateRange(action: RescheduleOccurrence) {
    check(
      action.start?.toLocalDate()?.isBefore(authorisation.start) != true &&
        action.end?.toLocalDate()?.isAfter(authorisation.end) != true,
    ) {
      "Temporary absence must be within the authorised date range."
    }
  }

  private fun RecategoriseOccurrence.recalculateCategorisation(): Pair<RecategoriseOccurrence, (KClass<out ReferenceData>, String) -> ReferenceData> {
    val (ca, allRd) = acr.getReasonCategorisation(this)
    val newAction = copy(
      absenceTypeCode = ca.absenceType?.code,
      absenceSubTypeCode = ca.absenceSubType?.code,
      absenceReasonCategoryCode = ca.absenceReasonCategory?.code,
      absenceReasonCode = ca.absenceReason.code,
    )
    return newAction to { domain: KClass<out ReferenceData>, code: String -> requireNotNull(allRd[domain to code]) }
  }
}
