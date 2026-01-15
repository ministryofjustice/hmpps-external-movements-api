package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangePrisonPerson
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DeferAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.DenyAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.RecategoriseAuthorisation
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
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(personIdentifier: String, request: TapAuthorisation): SyncResponse {
    val person = personSummaryService.getWithSave(personIdentifier)
    val rdPaths = referenceDataRepository.referenceDataFor(request)
    val application = (
      request.id?.let { authorisationRepository.findByIdOrNull(it) }
        ?: authorisationRepository.findByLegacyId(request.legacyId)
      )
      ?.also {
        request.updated?.also { ExternalMovementContext.get().copy(requestAt = it.at, username = it.by).set() }
      }
      ?.update(person, request, rdPaths)
      ?: let {
        ExternalMovementContext.get().copy(requestAt = request.created.at, username = request.created.by).set()
        authorisationRepository.save(request.asEntity(person, rdPaths))
      }
    return SyncResponse(application.id)
  }

  fun deleteById(id: UUID) {
    authorisationRepository.findByIdOrNull(id)?.also { authorisation ->
      val occurrences = occurrenceRepository.findByAuthorisationId(authorisation.id)
      val occurrenceIds = occurrences.map { it.id }.toSet()
      movementRepository.findByOccurrenceIdIn(occurrenceIds).also { movementRepository.deleteAll(it) }
      occurrenceRepository.deleteAll(occurrences)
      authorisationRepository.delete(authorisation)
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
      absenceType = absenceTypeCode?.let { rdPaths.getReferenceData(AbsenceType::class, it) as AbsenceType },
      absenceSubType = absenceSubTypeCode?.let {
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
      reasonPath = reasonPath,
      schedule = null,
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
    checkSchedule(request, rdPaths)
    checkStatus(request, rdPaths)
    request.comments?.also { applyComments(ChangeAuthorisationComments(it)) }
  }

  private fun TemporaryAbsenceAuthorisation.applyAbsenceCategorisation(
    request: TapAuthorisation,
    rdPaths: ReferenceDataPaths,
  ) {
    val categoryCode = rdPaths.reasonPath().path.singleOrNull { it.domain == ABSENCE_REASON_CATEGORY }?.code
    applyAbsenceCategorisation(
      RecategoriseAuthorisation(
        request.absenceTypeCode,
        request.absenceSubTypeCode,
        categoryCode,
        request.absenceReasonCode,
        rdPaths.reasonPath(),
      ),
      rdPaths::getReferenceData,
    )
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
