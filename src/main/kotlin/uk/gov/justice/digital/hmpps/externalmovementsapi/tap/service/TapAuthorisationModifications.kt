package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.forAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus.Code.PAUSED
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.AuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.AuthorisationActions
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ClearAuthorisationSchedule
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.CreateOccurrences
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.PauseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.authorisation.ResumeAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.asOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.history.AuthorisationHistory
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.KClass

@Service
class TapAuthorisationModifications(
  private val transactionTemplate: TransactionTemplate,
  private val taaRepository: TemporaryAbsenceAuthorisationRepository,
  private val taoRepository: TemporaryAbsenceOccurrenceRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val occurrenceStatusRepository: OccurrenceStatusRepository,
  private val acr: AbsenceCategorisationRetriever,
  private val authorisationHistory: AuthorisationHistory,
) {
  fun apply(id: UUID, action: AuthorisationAction): AuditHistory? {
    ExternalMovementContext.get().copy(reason = action.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val authorisation = taaRepository.getAuthorisation(id)
      val readVersion = authorisation.version
      authorisation.applyAction(action, referenceDataRepository.rdProvider())
      taaRepository.flush()
      readVersion!! to authorisation.version!!
    }
    return AuditHistory(listOfNotNull(authorisationHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun TemporaryAbsenceAuthorisation.applyAction(
    action: AuthorisationAction,
    rdSupplier: (KClass<out ReferenceData>, String) -> ReferenceData,
  ) {
    when (action) {
      is ApproveAuthorisation -> if (status.code !in listOf(PENDING.name, APPROVED.name)) {
        throw ConflictException(NOT_AWAITING_APPROVAL)
      } else {
        approve(action, rdSupplier)
        updateOccurrenceStatus()
      }

      is DenyAuthorisation -> if (status.code !in listOf(PENDING.name, DENIED.name)) {
        throw ConflictException(NOT_AWAITING_APPROVAL)
      } else {
        deny(action, rdSupplier)
        updateOccurrenceStatus()
      }

      is PauseAuthorisation -> if (status.code !in listOf(APPROVED.name, PAUSED.name)) {
        throw ConflictException(NOT_YET_APPROVED)
      } else {
        pause(action, rdSupplier)
        updateOccurrenceStatus()
      }

      is ResumeAuthorisation -> if (status.code !in listOf(APPROVED.name, PAUSED.name)) {
        throw ConflictException(NOT_YET_APPROVED)
      } else {
        resume(action, rdSupplier)
        updateOccurrenceStatus()
      }

      is CancelAuthorisation -> if (status.code !in listOf(APPROVED.name, PAUSED.name, CANCELLED.name)) {
        throw ConflictException(NOT_YET_APPROVED)
      } else {
        cancel(action, rdSupplier)
        affectedOccurrences().forEach {
          if (!repeat) {
            it.makeDpsOnly()
          }
          it.cancel(CancelOccurrence(), rdSupplier)
        }
      }

      is ClearAuthorisationSchedule -> if (status.code !in listOf(
          APPROVED.name,
          PAUSED.name,
          CANCELLED.name,
        )
      ) {
        throw ConflictException(NOT_YET_APPROVED)
      } else {
        cancel(action, rdSupplier)
        affectedOccurrences().forEach {
          if (repeat) {
            taoRepository.delete(it)
          } else {
            it.makeDpsOnly()
            it.cancel(CancelOccurrence(), rdSupplier)
          }
        }
      }

      is ChangeAuthorisationAccompaniment -> {
        applyAccompaniment(action, rdSupplier)
        val coa = ChangeOccurrenceAccompaniment(action.accompaniedByCode, action.reason)
        affectedOccurrences().forEach { it.applyAccompaniment(coa, rdSupplier) }
      }

      is ChangeAuthorisationTransport -> {
        applyTransport(action, rdSupplier)
        val coa = ChangeOccurrenceTransport(action.transportCode, action.reason)
        affectedOccurrences().forEach { it.applyTransport(coa, rdSupplier) }
      }

      is ChangeAuthorisationComments -> {
        applyComments(action)
        val aoc = ChangeOccurrenceComments(action.comments, action.reason)
        affectedOccurrences().forEach { it.applyComments(aoc) }
      }

      is ChangeAuthorisationDateRange -> {
        val odr = taoRepository.dateRangeForAuthorisation(id) ?: action
        check(!odr.start.isBefore(action.start) && !odr.end.isAfter(action.end)) {
          "Authorisation date range cannot be less than the date range of absences"
        }
        applyDateRange(action, rdSupplier)
      }

      is RecategoriseAuthorisation -> {
        val (action, rdSupplier) = action.recalculateCategorisation()
        applyAbsenceCategorisation(action, rdSupplier)
        val ro = RecategoriseOccurrence(action)
        affectedOccurrences().forEach { it.applyAbsenceCategorisation(ro, rdSupplier) }
      }

      is ChangeAuthorisationLocation -> {
        val occurrences = taoRepository.findByAuthorisationId(id)
        occurrences.filter { it.end.isAfter(LocalDateTime.now()) }.forEach {
          it.applyLocation(ChangeOccurrenceLocation(action.location))
        }
        val allLocations = occurrences.mapTo(linkedSetOf()) { it.location }
        applyLocations(ChangeAuthorisationLocations(allLocations))
      }

      is CreateOccurrences -> {
        if (!permitsOccurrences()) {
          throw ConflictException("Cannot add a new occurrence to a non active authorisation")
        }
        val occurrenceStatuses = occurrenceStatusRepository.findAll().associateBy { it.code }
        val occurrences = action.occurrences.map { occ ->
          check(!occ.start.toLocalDate().isBefore(start) && !occ.end.toLocalDate().isAfter(end)) {
            "Temporary absence must be within the authorised date range."
          }

          if (!repeat) {
            check(action.occurrences.size == 1 && taoRepository.countByAuthorisationId(id) == 0) {
              "Cannot add multiple occurrences to a single authorisation."
            }
          }

          occ.asOccurrence(this).calculateStatus { requireNotNull(occurrenceStatuses[it]) }
        }

        val locations = (locations + occurrences.map { it.location }).mapTo(linkedSetOf()) { it }
        applyLocations(ChangeAuthorisationLocations(locations))
        taoRepository.saveAll(occurrences)
      }

      else -> throw IllegalArgumentException("Action not supported")
    }
  }

  fun apply(id: UUID, request: AuthorisationActions): AuditHistory? {
    ExternalMovementContext.get().copy(reason = request.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val authorisation = taaRepository.getAuthorisation(id)
      val readVersion = authorisation.version
      val rdSupplier = referenceDataRepository.rdProvider()
      request.actions.forEach { authorisation.applyAction(it, rdSupplier) }
      taaRepository.flush()
      readVersion!! to authorisation.version!!
    }
    return AuditHistory(listOfNotNull(authorisationHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun TemporaryAbsenceAuthorisation.affectedOccurrences() = taoRepository.findAll(
    forAuthorisation(id)
      .and(
        occurrenceStatusCodeIn(
          OccurrenceStatus.Code.PENDING,
          OccurrenceStatus.Code.SCHEDULED,
          OccurrenceStatus.Code.PAUSED,
        ),
      ),
  )

  private fun TemporaryAbsenceAuthorisation.updateOccurrenceStatus() {
    affectedOccurrences().forEach {
      if (status.code == PAUSED.name || (!repeat && status.code != APPROVED.name)) {
        it.makeDpsOnly()
      }
      it.calculateStatus { statusCode -> occurrenceStatusRepository.getByCode(statusCode) }
    }
  }

  private fun RecategoriseAuthorisation.recalculateCategorisation(): Pair<RecategoriseAuthorisation, (KClass<out ReferenceData>, String) -> ReferenceData> {
    val (ca, allRd) = acr.getReasonCategorisation(this)
    val newAction = copy(
      absenceTypeCode = ca.absenceType?.code,
      absenceSubTypeCode = ca.absenceSubType?.code,
      absenceReasonCategoryCode = ca.absenceReasonCategory?.code,
      absenceReasonCode = ca.absenceReason.code,
    )
    return newAction to { domain: KClass<out ReferenceData>, code: String -> requireNotNull(allRd[domain to code]) }
  }

  companion object {
    const val NOT_AWAITING_APPROVAL = "Temporary absence authorisation not awaiting approval"
    const val NOT_YET_APPROVED = "Temporary absence authorisation not approved"
  }
}
