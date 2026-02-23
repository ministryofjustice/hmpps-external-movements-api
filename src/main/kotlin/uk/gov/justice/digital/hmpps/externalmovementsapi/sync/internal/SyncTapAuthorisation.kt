package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationAccompaniment
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationTransport
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangePrisonPerson
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DeferAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence.RescheduleOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.isNullOrEmpty
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.SyncResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.TapAuthorisation
import java.time.LocalDate.now
import java.util.UUID

@Transactional
@Service
class SyncTapAuthorisation(
  private val personSummaryService: PersonSummaryService,
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val objectMapper: ObjectMapper,
) {
  fun sync(personIdentifier: String, request: TapAuthorisation): SyncResponse {
    val person = personSummaryService.getWithSave(personIdentifier)
    val rdPaths = referenceDataRepository.referenceDataFor(request)
    val authorisation = (
      request.id?.let { authorisationRepository.findByIdOrNull(it) }
        ?: authorisationRepository.findByLegacyId(request.legacyId)
      )
      ?.also {
        request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
      }
      ?.update(person, request, rdPaths)
      ?: let {
        ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
        val saved = authorisationRepository.save(request.asEntity(person, rdPaths))
        if (!saved.repeat && saved.schedule != null && saved.status.code != AuthorisationStatus.Code.APPROVED.name) {
          saved.createOccurrence(objectMapper, rdPaths)
        }
        saved
      }
    return SyncResponse(authorisation.id)
  }

  fun deleteById(id: UUID) {
    ExternalMovementContext.get().copy(username = SYSTEM_USERNAME).set()
    authorisationRepository.findByIdOrNull(id)?.also { authorisation ->
      val occurrenceCount = occurrenceRepository.countByAuthorisationId(authorisation.id)
      if (occurrenceCount > 0) {
        throw ConflictException("Cannot delete an authorisation with a scheduled occurrence")
      } else {
        authorisationRepository.delete(authorisation)
      }
    }
  }

  private fun TapAuthorisation.asEntity(
    person: PersonSummary,
    rdPaths: ReferenceDataPaths,
  ): TemporaryAbsenceAuthorisation {
    val reasonPath = rdPaths.reasonPath()
    val category = reasonPath.path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.let {
      rdPaths.getReferenceData(it.domain.clazz, it.code)
    }
    return TemporaryAbsenceAuthorisation(
      person = person,
      prisonCode = prisonCode,
      status = if (end.isBefore(now()) && statusCode == AuthorisationStatus.Code.PENDING.name) {
        rdPaths.getReferenceData(AuthorisationStatus::class, EXPIRED.name) as AuthorisationStatus
      } else {
        rdPaths.getReferenceData(AuthorisationStatus::class, statusCode) as AuthorisationStatus
      },
      absenceType = absenceTypeCode?.let {
        rdPaths.getReferenceData(AbsenceType::class, it) as AbsenceType
      },
      absenceSubType = absenceSubTypeCode?.takeIf { it != AbsenceSubType.Code.SECURITY_ESCORT.value }?.let {
        rdPaths.getReferenceData(AbsenceSubType::class, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(AbsenceReason::class, absenceReasonCode) as AbsenceReason,
      accompaniedBy = rdPaths.getReferenceData(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(Transport::class, transportCode) as Transport,
      repeat = repeat,
      comments = comments,
      start = start,
      end = end,
      locations = location?.takeUnless(Location::isNullOrEmpty)?.let { linkedSetOf(it) } ?: linkedSetOf(),
      reasonPath = reasonPath,
      schedule = schedule()?.let { objectMapper.valueToTree(it) },
      legacyId = legacyId,
      id = id ?: newUuid(),
    )
  }

  fun TemporaryAbsenceAuthorisation.update(
    person: PersonSummary,
    request: TapAuthorisation,
    rdPaths: ReferenceDataPaths,
  ) = apply {
    applyPrisonPerson(ChangePrisonPerson(person.identifier, request.prisonCode)) { person }
    applyAbsenceCategorisation(request, rdPaths)
    checkStatus(request, rdPaths)
    checkSchedule(request, rdPaths)
    applyLogistics(request, rdPaths)
    applyComments(ChangeAuthorisationComments(request.comments))
    val occurrences = occurrenceRepository.findByAuthorisationId(id)
    (
      occurrences.mapTo(linkedSetOf()) { it.location }.takeIf { it.isNotEmpty() }
        ?: request.location?.takeIf { it.isNullOrEmpty() }?.let { linkedSetOf(it) }
      )?.also { applyLocations(ChangeAuthorisationLocations(it)) }
    val schedule = request.schedule()?.also { applySchedule(objectMapper.valueToTree(it)) }
    if (schedule != null && !repeat && status.code != AuthorisationStatus.Code.APPROVED.name) {
      occurrences.singleOrNull()?.let { occ ->
        if (occ.status.code == OccurrenceStatus.Code.SCHEDULED.name) {
          occurrenceRepository.delete(occ)
          null
        } else {
          occ.reschedule(RescheduleOccurrence(start.atTime(schedule.startTime), end.atTime(schedule.returnTime)))
          occ.calculateStatus { rdPaths.getReferenceData(OccurrenceStatus::class, it) as OccurrenceStatus }
        }
      } ?: createOccurrence(objectMapper, rdPaths)
    }
  }

  private fun TemporaryAbsenceAuthorisation.createOccurrence(
    objectMapper: ObjectMapper,
    rdPaths: ReferenceDataPaths,
  ) {
    occurrence(objectMapper)?.calculateStatus { code ->
      rdPaths.getReferenceData(OccurrenceStatus::class, code) as OccurrenceStatus
    }?.also(occurrenceRepository::save)
  }

  private fun TemporaryAbsenceAuthorisation.applyAbsenceCategorisation(
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

  private fun TemporaryAbsenceAuthorisation.applyLogistics(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
    applyAccompaniment(ChangeAuthorisationAccompaniment(request.accompaniedByCode), rdPaths::getReferenceData)
    applyTransport(ChangeAuthorisationTransport(request.transportCode), rdPaths::getReferenceData)
  }

  private fun TemporaryAbsenceAuthorisation.checkStatus(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
    when (request.statusCode) {
      AuthorisationStatus.Code.PENDING.name -> defer(DeferAuthorisation(), rdPaths::getReferenceData)
      AuthorisationStatus.Code.APPROVED.name -> approve(ApproveAuthorisation(), rdPaths::getReferenceData)
      AuthorisationStatus.Code.CANCELLED.name -> cancel(CancelAuthorisation(), rdPaths::getReferenceData)
      AuthorisationStatus.Code.DENIED.name -> deny(DenyAuthorisation(), rdPaths::getReferenceData)
    }
  }

  private fun TemporaryAbsenceAuthorisation.checkSchedule(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
    applyDateRange(ChangeAuthorisationDateRange(request.start, request.end), rdPaths::getReferenceData)
  }
}
