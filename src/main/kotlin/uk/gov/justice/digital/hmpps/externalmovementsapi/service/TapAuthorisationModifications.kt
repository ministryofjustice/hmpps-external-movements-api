package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.forAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.occurrenceStatusCodeIn
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditHistory
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AmendAuthorisationNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.AmendOccurrenceNotes
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.history.AuthorisationHistory
import java.util.UUID

@Service
class TapAuthorisationModifications(
  private val transactionTemplate: TransactionTemplate,
  private val taaRepository: TemporaryAbsenceAuthorisationRepository,
  private val taoRepository: TemporaryAbsenceOccurrenceRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationHistory: AuthorisationHistory,
) {
  fun apply(id: UUID, action: AuthorisationAction): AuditHistory? {
    ExternalMovementContext.get().copy(reason = action.reason).set()
    val (readVersion, writeVersion) = transactionTemplate.execute {
      val authorisation = taaRepository.getAuthorisation(id)
      val readVersion = authorisation.version
      val rdSupplier =
        { domain: ReferenceDataDomain.Code, code: String -> referenceDataRepository.getByKey(domain of code) }
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

        is AmendAuthorisationNotes -> {
          authorisation.amendNotes(action)
          val aon = AmendOccurrenceNotes(action.notes, action.reason)
          authorisation.affectedOccurrences().forEach { it.amendNotes(aon) }
        }

        is ChangeAuthorisationDateRange -> {
          val odr = taoRepository.dateRangeForAuthorisation(authorisation.id) ?: action
          check(!odr.fromDate.isBefore(action.fromDate) && !odr.toDate.isAfter(action.toDate)) {
            "Authorisation date range cannot be less than the date range of absences"
          }
          authorisation.amendDateRange(action)
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
        occurrenceStatusCodeIn(TapOccurrenceStatus.Code.PENDING, TapOccurrenceStatus.Code.SCHEDULED),
      ),
  )

  private fun TemporaryAbsenceAuthorisation.updateOccurrenceStatus() {
    affectedOccurrences().forEach {
      it.calculateStatus { statusCode ->
        referenceDataRepository.findByKey(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS of statusCode) as TapOccurrenceStatus
      }
    }
  }

  private fun RecategoriseAuthorisation.recalculateCategorisation(): Pair<RecategoriseAuthorisation, (ReferenceDataDomain.Code, String) -> ReferenceData> {
    val rdProvider = referenceDataRepository.rdProvider(this)
    val linkProvider = { previous: ReferenceData ->
      referenceDataRepository.findLinkedItems(previous.id).let {
        when (it.size) {
          1 -> it.single()
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val type = requireNotNull(absenceTypeCode?.let { rdProvider(ABSENCE_TYPE, it) as AbsenceType })
    val subType = (absenceSubTypeCode?.let { rdProvider(ABSENCE_SUB_TYPE, it) } ?: linkProvider(type)) as? AbsenceSubType
    val reasonCategory = (absenceReasonCategoryCode?.let { rdProvider(ABSENCE_REASON_CATEGORY, it) }) as? AbsenceReasonCategory
    val reason = (absenceReasonCode?.let { rdProvider(ABSENCE_REASON, it) } ?: linkProvider(reasonCategory ?: subType ?: type)) as? AbsenceReason
    val newAction = copy(
      absenceTypeCode = type.code,
      absenceSubTypeCode = subType?.code,
      absenceReasonCategoryCode = reasonCategory?.code,
      absenceReasonCode = reason?.code,
    )
    return newAction to referenceDataRepository.rdProvider(newAction)
  }
}
