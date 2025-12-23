package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.forAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.history.AuthorisationHistory
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
      val rdSupplier = referenceDataRepository.rdProvider()
      when (action) {
        is ApproveAuthorisation -> if (authorisation.status.code !in listOf(PENDING.name, APPROVED.name)) {
          throw ConflictException("Temporary absence authorisation not awaiting approval")
        } else {
          authorisation.approve(action, rdSupplier)
          authorisation.updateOccurrenceStatus()
        }

        is DenyAuthorisation -> if (authorisation.status.code !in listOf(PENDING.name, DENIED.name)) {
          throw ConflictException("Temporary absence authorisation not awaiting approval")
        } else {
          authorisation.deny(action, rdSupplier)
          authorisation.updateOccurrenceStatus()
        }

        is CancelAuthorisation -> if (authorisation.status.code !in listOf(APPROVED.name, CANCELLED.name)) {
          throw ConflictException("Temporary absence authorisation not approved")
        } else {
          authorisation.cancel(action, rdSupplier)
          authorisation.updateOccurrenceStatus()
        }

        is ChangeAuthorisationAccompaniment -> {
          authorisation.applyAccompaniment(action, rdSupplier)
          val coa = ChangeOccurrenceAccompaniment(action.accompaniedByCode, action.reason)
          authorisation.affectedOccurrences().forEach { it.applyAccompaniment(coa, rdSupplier) }
        }

        is ChangeAuthorisationTransport -> {
          authorisation.applyTransport(action, rdSupplier)
          val coa = ChangeOccurrenceTransport(action.transportCode, action.reason)
          authorisation.affectedOccurrences().forEach { it.applyTransport(coa, rdSupplier) }
        }

        is ChangeAuthorisationComments -> {
          authorisation.applyComments(action)
          val aoc = ChangeOccurrenceComments(action.comments, action.reason)
          authorisation.affectedOccurrences().forEach { it.applyComments(aoc) }
        }

        is ChangeAuthorisationDateRange -> {
          val odr = taoRepository.dateRangeForAuthorisation(authorisation.id) ?: action
          check(!odr.start.isBefore(action.start) && !odr.end.isAfter(action.end)) {
            "Authorisation date range cannot be less than the date range of absences"
          }
          authorisation.applyDateRange(action, rdSupplier)
        }

        is RecategoriseAuthorisation -> {
          val (action, rdSupplier) = action.recalculateCategorisation()
          authorisation.applyAbsenceCategorisation(action, rdSupplier)
          val ro = RecategoriseOccurrence(action)
          authorisation.affectedOccurrences().forEach { it.applyAbsenceCategorisation(ro, rdSupplier) }
        }

        else -> throw IllegalArgumentException("Action not supported")
      }
      taaRepository.flush()
      readVersion!! to authorisation.version!!
    }!!
    return AuditHistory(listOfNotNull(authorisationHistory.currentAction(id, readVersion, writeVersion)))
  }

  private fun TemporaryAbsenceAuthorisation.affectedOccurrences() = taoRepository.findAll(
    forAuthorisation(id)
      .and(
        occurrenceStatusCodeIn(OccurrenceStatus.Code.PENDING, OccurrenceStatus.Code.SCHEDULED),
      ),
  )

  private fun TemporaryAbsenceAuthorisation.updateOccurrenceStatus() {
    affectedOccurrences().forEach {
      it.calculateStatus { statusCode -> occurrenceStatusRepository.getByCode(statusCode) }
    }
  }

  private fun RecategoriseAuthorisation.recalculateCategorisation(): Pair<RecategoriseAuthorisation, (KClass<out ReferenceData>, String) -> ReferenceData> {
    val (ca, allRd) = acr.getReasonCategorisation(this)
    val newAction = copy(
      absenceTypeCode = ca.absenceType?.code,
      absenceSubTypeCode = ca.absenceSubType?.code,
      absenceReasonCategoryCode = ca.absenceReasonCategory?.code,
      absenceReasonCode = ca.absenceReason?.code,
    )
    return newAction to { domain: KClass<out ReferenceData>, code: String -> requireNotNull(allRd[domain to code]) }
  }
}
