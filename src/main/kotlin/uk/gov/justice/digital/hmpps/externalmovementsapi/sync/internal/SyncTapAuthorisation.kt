package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.APPROVED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.CANCELLED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.DENIED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.findRdWithPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ApproveAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.CancelAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationComments
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangePrisonPerson
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
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryService: PersonSummaryService,
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun sync(personIdentifier: String, request: TapAuthorisation): SyncResponse {
    val prisoner = prisonerSearch.getPrisoner(personIdentifier) ?: throw NotFoundException("Prisoner not found")
    val rdPaths = referenceDataRepository.findRdWithPaths(request)
    val person = personSummaryService.save(prisoner)
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
      rdPaths.getReferenceData(it.domain, it.code)
    }
    return TemporaryAbsenceAuthorisation(
      id = id ?: newUuid(),
      person = person,
      prisonCode = prisonCode,
      absenceType = absenceTypeCode?.let { rdPaths.getReferenceData(ABSENCE_TYPE, it) as AbsenceType },
      absenceSubType = absenceSubTypeCode?.let {
        rdPaths.getReferenceData(ABSENCE_SUB_TYPE, it) as AbsenceSubType
      },
      absenceReasonCategory = category as? AbsenceReasonCategory,
      absenceReason = rdPaths.getReferenceData(ABSENCE_REASON, absenceReasonCode) as AbsenceReason,
      accompaniedBy = rdPaths.getReferenceData(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
      transport = rdPaths.getReferenceData(TRANSPORT, transportCode) as Transport,
      repeat = repeat,
      status = if (end.isBefore(now()) && statusCode == TapAuthorisationStatus.Code.PENDING.name) {
        rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, EXPIRED.name) as TapAuthorisationStatus
      } else {
        rdPaths.getReferenceData(TAP_AUTHORISATION_STATUS, statusCode) as TapAuthorisationStatus
      },
      comments = comments,
      start = start,
      end = end,
      schedule = null,
      reasonPath = reasonPath,
      legacyId = legacyId,
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
      APPROVED.name -> approve(ApproveAuthorisation(), rdPaths::getReferenceData)
      CANCELLED.name -> cancel(CancelAuthorisation(), rdPaths::getReferenceData)
      DENIED.name -> deny(DenyAuthorisation(), rdPaths::getReferenceData)
    }
  }

  private fun TemporaryAbsenceAuthorisation.checkSchedule(request: TapAuthorisation, rdPaths: ReferenceDataPaths) {
    applyDateRange(ChangeAuthorisationDateRange(request.start, request.end), rdPaths::getReferenceData)
  }
}
