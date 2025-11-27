package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus.Code.SCHEDULED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.AuditedAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.AuthorisationAction
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
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
  fun apply(id: UUID, action: AuthorisationAction): AuditedAction {
    ExternalMovementContext.get().copy(reason = action.reason).set()
    val readVersion = transactionTemplate.execute {
      val authorisation = taaRepository.getAuthorisation(id)
      val readVersion = authorisation.version
      val rdSupplier =
        { domain: ReferenceDataDomain.Code, code: String -> referenceDataRepository.getByKey(domain of code) }
      when (action) {
        is ApproveAuthorisation -> if (authorisation.status.code !in listOf(PENDING.name, APPROVED.name)) {
          throw ConflictException("Temporary absence authorisation not awaiting approval")
        } else {
          authorisation.approve(action, rdSupplier)
          authorisation.updateOccurrences()
        }

        is DenyAuthorisation -> if (authorisation.status.code !in listOf(PENDING.name, DENIED.name)) {
          throw ConflictException("Temporary absence authorisation not awaiting approval")
        } else {
          authorisation.deny(action, rdSupplier)
          authorisation.updateOccurrences()
        }

        is CancelAuthorisation -> if (authorisation.status.code !in listOf(SCHEDULED.name, CANCELLED.name)) {
          throw ConflictException("Temporary absence authorisation not currently scheduled")
        } else {
          authorisation.cancel(action, rdSupplier)
          authorisation.updateOccurrences()
        }

        else -> throw IllegalArgumentException("Action not supported")
      }
      readVersion
    }!!
    return authorisationHistory.currentAction(id, readVersion)
  }

  private fun TemporaryAbsenceAuthorisation.updateOccurrences() {
    taoRepository.findByAuthorisationId(id).forEach {
      it.calculateStatus { statusCode ->
        referenceDataRepository.findByKey(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS of statusCode) as TapOccurrenceStatus
      }
    }
  }
}
