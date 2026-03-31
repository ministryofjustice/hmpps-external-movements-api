package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import kotlin.reflect.KClass

@Transactional
@Service
class ExternalMovementHandler(
  private val prc: PrisonRegisterClient,
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val authStatusRepository: AuthorisationStatusRepository,
  private val occRepository: TemporaryAbsenceOccurrenceRepository,
  private val occStatusRepository: OccurrenceStatusRepository,
) {
  fun handle(pme: ExternalMovementRecordedEvent) {
    if (pme.offenderIdDisplay == null || pme.toAgencyLocationId == null || pme.cancellationReason() == null) return
    ExternalMovementContext.get().copy(requestAt = pme.movementDateTime ?: now(), reason = pme.cancellationReason()).set()
    val authCancelledStatus = authStatusRepository.getByCode(AuthorisationStatus.Code.CANCELLED.name)
    val authsToCancel = authRepository.findAutoCancelAuthorisations(pme.offenderIdDisplay, pme.toAgencyLocationId)
    authsToCancel.forEach {
      it.cancel(CancelAuthorisation()) { _: KClass<out ReferenceData>, _: String -> authCancelledStatus }
    }
    val occCancelledStatus = occStatusRepository.getByCode(OccurrenceStatus.Code.CANCELLED.name)
    occRepository.findCancellableOccurrences(authsToCancel.map { it.id }.toSet()).forEach {
      if (it.authorisation.repeat) {
        occRepository.delete(it)
      } else {
        it.makeDpsOnly()
        it.cancel(CancelOccurrence()) { _: KClass<out ReferenceData>, _: String -> occCancelledStatus }
      }
    }
  }

  private fun ExternalMovementRecordedEvent.cancellationReason(): String? = when (directionCode to movementType) {
    "OUT" to "TRN", "IN" to "ADM" -> toAgencyLocationId?.takeIf { it.isPrison() }?.let { "Transferred" }
    "OUT" to "REL" -> "Released"
    else -> null
  }

  private fun String.isPrison(): Boolean = prc.findPrison(this) != null
}

data class ExternalMovementRecordedEvent(
  val offenderIdDisplay: String? = null,
  val movementType: String? = null,
  val directionCode: String? = null,
  val toAgencyLocationId: String? = null,
  val movementDateTime: LocalDateTime? = null,
) {
  companion object {
    const val EVENT_TYPE = "EXTERNAL_MOVEMENT_RECORD-INSERTED"
  }
}
