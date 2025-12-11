package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.rdProvider
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
import java.util.UUID

@Transactional
@Service
class CreateScheduledAbsence(
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryService: PersonSummaryService,
  private val referenceDataRepository: ReferenceDataRepository,
  private val tapAuthRepository: TemporaryAbsenceAuthorisationRepository,
  private val tapOccurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun tapAuthorisation(personIdentifier: String, request: CreateTapAuthorisationRequest): ReferenceId {
    val prisoner = prisonerSearch.getPrisoner(personIdentifier) ?: throw NotFoundException("Prisoner not found")
    val rdProvider = referenceDataRepository.rdProvider(request)
    val linkProvider = { nextDomain: ReferenceDataDomain.Code, previous: ReferenceData ->
      referenceDataRepository.findLinkedItems(nextDomain, previous.id).let {
        when (it.size) {
          0 -> null
          1 -> it.single()
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val person = personSummaryService.save(prisoner)
    val authorisation = tapAuthRepository.save(
      request.asAuthorisation(person, prisoner.lastPrisonId, rdProvider, linkProvider),
    )
    tapOccurrenceRepository.saveAll(
      request.occurrences.map { it.asOccurrence(authorisation, rdProvider, request) },
    )
    return ReferenceId(authorisation.id)
  }

  fun tapOccurrence(authorisationId: UUID, request: CreateOccurrenceRequest): ReferenceId {
    val authorisation = tapAuthRepository.getAuthorisation(authorisationId)
    check(
      !request.start.toLocalDate().isBefore(authorisation.start) &&
        !request.end.toLocalDate().isAfter(authorisation.end),
    ) {
      "Temporary absence must be within the authorised date range."
    }

    val occurrence = request.asOccurrence(authorisation).calculateStatus {
      referenceDataRepository.findByKey(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS of it) as TapOccurrenceStatus
    }

    return ReferenceId(tapOccurrenceRepository.save(occurrence).id)
  }

  private fun CreateTapAuthorisationRequest.asAuthorisation(
    person: PersonSummary,
    prisonCode: String,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
    linkProvider: (ReferenceDataDomain.Code, ReferenceData) -> ReferenceData?,
  ): TemporaryAbsenceAuthorisation {
    val type = rdProvider(ABSENCE_TYPE, absenceTypeCode) as AbsenceType
    val subType = (
      absenceSubTypeCode?.let { rdProvider(ABSENCE_SUB_TYPE, it) }
        ?: linkProvider(ABSENCE_SUB_TYPE, type)
      ) as? AbsenceSubType
    val reasonCategory = (
      absenceReasonCategoryCode?.let { rdProvider(ABSENCE_REASON_CATEGORY, it) }
      ) as AbsenceReasonCategory?
    val reason = (
      absenceReasonCode?.let { rdProvider(ABSENCE_REASON, it) }
        ?: linkProvider(ABSENCE_REASON, reasonCategory ?: subType ?: type)
      ) as AbsenceReason
    return TemporaryAbsenceAuthorisation(
      person = person,
      prisonCode = prisonCode,
      absenceType = type,
      absenceSubType = subType,
      absenceReasonCategory = reasonCategory,
      absenceReason = reason,
      status = rdProvider(TAP_AUTHORISATION_STATUS, statusCode.name) as TapAuthorisationStatus,
      comments = comments,
      start = start,
      end = end,
      repeat = repeat,
      reasonPath = ReasonPath(reasonPath()),
      schedule = schedule,
      accompaniedBy = rdProvider(ACCOMPANIED_BY, accompaniedByCode) as AccompaniedBy,
      transport = rdProvider(TRANSPORT, transportCode) as Transport,
      legacyId = null,
    )
  }

  private fun CreateTapAuthorisationRequest.OccurrenceRequest.asOccurrence(
    authorisation: TemporaryAbsenceAuthorisation,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
    authRequest: CreateTapAuthorisationRequest,
  ): TemporaryAbsenceOccurrence = TemporaryAbsenceOccurrence(
    authorisation,
    absenceType = authorisation.absenceType,
    absenceSubType = authorisation.absenceSubType,
    absenceReasonCategory = authorisation.absenceReasonCategory,
    absenceReason = authorisation.absenceReason,
    start = start,
    end = end,
    accompaniedBy = authorisation.accompaniedBy,
    transport = authorisation.transport,
    location = location.let {
      if (it.address?.isEmpty() == true) {
        it.copy(address = null)
      } else {
        it
      }
    },
    contactInformation = authRequest.contactInformation,
    comments = authorisation.comments,
    reasonPath = authorisation.reasonPath,
    scheduleReference = scheduleReference,
    legacyId = null,
  ).calculateStatus { rdProvider(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS, it) as TapOccurrenceStatus }

  private fun CreateOccurrenceRequest.asOccurrence(
    authorisation: TemporaryAbsenceAuthorisation,
  ): TemporaryAbsenceOccurrence = TemporaryAbsenceOccurrence(
    authorisation,
    absenceType = authorisation.absenceType,
    absenceSubType = authorisation.absenceSubType,
    absenceReasonCategory = authorisation.absenceReasonCategory,
    absenceReason = authorisation.absenceReason,
    start = start,
    end = end,
    accompaniedBy = authorisation.accompaniedBy,
    transport = authorisation.transport,
    location = location.let {
      if (it.address?.isEmpty() == true) {
        it.copy(address = null)
      } else {
        it
      }
    },
    contactInformation = null,
    comments = comments ?: authorisation.comments,
    reasonPath = authorisation.reasonPath,
    scheduleReference = null,
    legacyId = null,
  )
}
