package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.treeToValue
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DeferAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ExpireAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.CancelOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceContactInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.ChangeOccurrenceTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RecategoriseOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.migrate.TapOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.AuthorisationSchedule
import java.time.LocalDate.now

internal fun TemporaryAbsenceAuthorisation.applyAbsenceCategorisation(
  request: TapAuthorisation,
  rdPaths: ReferenceDataPaths,
) {
  val categoryCode = rdPaths.reasonPath().path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.code
  applyAbsenceCategorisation(
    RecategoriseAuthorisation(
      request.absenceTypeCode,
      request.absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value },
      categoryCode,
      request.absenceReasonCode,
      rdPaths.reasonPath(),
    ),
    rdPaths::getReferenceData,
  )
}

internal fun TemporaryAbsenceAuthorisation.applyLogistics(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
  applyAccompaniment(ChangeAuthorisationAccompaniment(request.accompaniedByCode), rdPaths::getReferenceData)
  applyTransport(ChangeAuthorisationTransport(request.transportCode), rdPaths::getReferenceData)
}

internal fun TemporaryAbsenceAuthorisation.checkStatus(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
  when (request.statusCode) {
    EXPIRED.name -> expire(ExpireAuthorisation(), rdPaths::getReferenceData)
    PENDING.name if (now().isAfter(request.end)) -> expire(ExpireAuthorisation(), rdPaths::getReferenceData)
    PENDING.name -> defer(DeferAuthorisation(), rdPaths::getReferenceData)
    APPROVED.name -> approve(ApproveAuthorisation(), rdPaths::getReferenceData)
    CANCELLED.name -> cancel(CancelAuthorisation(), rdPaths::getReferenceData)
    DENIED.name -> deny(DenyAuthorisation(), rdPaths::getReferenceData)
  }
}

internal fun TemporaryAbsenceAuthorisation.checkSchedule(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
  applyDateRange(ChangeAuthorisationDateRange(request.start, request.end), rdPaths::getReferenceData)
}

internal fun TemporaryAbsenceOccurrence.applyAbsenceCategorisation(
  request: TapOccurrence,
  rdPaths: ReferenceDataPaths,
) {
  val categoryCode = rdPaths.reasonPath().path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.code
  applyAbsenceCategorisation(
    RecategoriseOccurrence(
      request.absenceTypeCode,
      request.absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value },
      categoryCode,
      request.absenceReasonCode,
      rdPaths.reasonPath(),
    ),
    rdPaths::getReferenceData,
  )
}

internal fun TemporaryAbsenceOccurrence.applySchedule(request: TapOccurrence) {
  reschedule(RescheduleOccurrence(request.start, request.end))
}

internal fun TemporaryAbsenceOccurrence.applyLogistics(request: TapOccurrence, rdPaths: ReferenceDataPaths) {
  applyLocation(ChangeOccurrenceLocation(request.location))
  applyAccompaniment(ChangeOccurrenceAccompaniment(request.accompaniedByCode), rdPaths::getReferenceData)
  applyTransport(ChangeOccurrenceTransport(request.transportCode), rdPaths::getReferenceData)
  request.contactInformation?.also { applyContactInformation(ChangeOccurrenceContactInformation(it)) }
}

internal fun TemporaryAbsenceOccurrence.checkCancellation(request: TapOccurrence, rdPaths: ReferenceDataPaths) {
  if (request.isCancelled) {
    cancel(CancelOccurrence(), rdPaths::getReferenceData)
  }
}

fun TemporaryAbsenceAuthorisation.occurrence(jsonMapper: JsonMapper): TemporaryAbsenceOccurrence? = this.schedule?.let {
  val schedule = jsonMapper.treeToValue<AuthorisationSchedule>(it)
  TemporaryAbsenceOccurrence(
    authorisation = this,
    absenceType = absenceType,
    absenceSubType = absenceSubType,
    absenceReasonCategory = absenceReasonCategory,
    absenceReason = absenceReason,
    start = start.atTime(schedule.startTime),
    end = end.atTime(schedule.returnTime),
    contactInformation = null,
    accompaniedBy = accompaniedBy,
    transport = transport,
    location = locations.firstOrNull() ?: Location.empty(),
    comments = comments,
    reasonPath = reasonPath,
    scheduleReference = null,
    legacyId = null,
    dpsOnly = true,
  )
}
