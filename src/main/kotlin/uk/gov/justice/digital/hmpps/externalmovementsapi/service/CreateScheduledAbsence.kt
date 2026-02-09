package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummary
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLinkRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.AbsenceCategorisationException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ChangeAuthorisationLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService
import java.util.UUID
import kotlin.reflect.KClass

@Transactional
@Service
class CreateScheduledAbsence(
  private val prisonerSearch: PrisonerSearchClient,
  private val personSummaryService: PersonSummaryService,
  private val referenceDataRepository: ReferenceDataRepository,
  private val absenceCategorisationLinkRepository: AbsenceCategorisationLinkRepository,
  private val occurrenceStatusRepository: OccurrenceStatusRepository,
  private val tapAuthRepository: TemporaryAbsenceAuthorisationRepository,
  private val tapOccurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun tapAuthorisation(personIdentifier: String, request: CreateTapAuthorisationRequest): ReferenceId {
    val prisoner = prisonerSearch.getPrisoner(personIdentifier) ?: throw NotFoundException("Prisoner not found")
    val allRd = referenceDataRepository.findAll().associateBy { it::class to it.code }
    val rdProvider = { clazz: KClass<out ReferenceData>, code: String -> requireNotNull(allRd[clazz to code]) }
    val linkProvider = { nextDomain: ReferenceDataDomain.Code, previous: ReferenceData ->
      absenceCategorisationLinkRepository.findById1AndDomain2(previous.id, nextDomain).let {
        when (it.size) {
          0 -> null
          1 -> allRd.values.first { rd -> rd.id == it.single().id2 }
          else -> throw AbsenceCategorisationException(previous, it.size)
        }
      }
    }
    val person = personSummaryService.save(prisoner)
    val authorisation = tapAuthRepository.save(
      request.asAuthorisation(person, prisoner.lastPrisonId ?: Prisoner.UNKNOWN_PRISON, rdProvider, linkProvider),
    )
    tapOccurrenceRepository.saveAll(
      request.occurrences.map { it.asOccurrence(authorisation, rdProvider, request) },
    )
    return ReferenceId(authorisation.id)
  }

  fun tapOccurrence(authorisationId: UUID, request: CreateOccurrenceRequest): ReferenceId {
    val authorisation = tapAuthRepository.getAuthorisation(authorisationId)
    if (!authorisation.permitsOccurrences()) {
      throw ConflictException("Cannot add a new occurrence to a non active authorisation")
    }
    check(
      !request.start.toLocalDate().isBefore(authorisation.start) &&
        !request.end.toLocalDate().isAfter(authorisation.end),
    ) {
      "Temporary absence must be within the authorised date range."
    }
    if (!authorisation.repeat) {
      check(tapOccurrenceRepository.countByAuthorisationId(authorisationId) == 0) {
        "Cannot add multiple occurrences to a single authorisation."
      }
    }

    val occurrence = request.asOccurrence(authorisation).calculateStatus { occurrenceStatusRepository.getByCode(it) }
    val locations = (authorisation.locations + occurrence.location).mapTo(linkedSetOf()) { it }
    authorisation.applyLocations(ChangeAuthorisationLocations(locations)).clearSchedule()

    return ReferenceId(tapOccurrenceRepository.save(occurrence).id)
  }

  private fun CreateTapAuthorisationRequest.asAuthorisation(
    person: PersonSummary,
    prisonCode: String,
    rdProvider: (KClass<out ReferenceData>, String) -> ReferenceData,
    linkProvider: (ReferenceDataDomain.Code, ReferenceData) -> ReferenceData?,
  ): TemporaryAbsenceAuthorisation {
    val type = rdProvider(AbsenceType::class, absenceTypeCode) as AbsenceType
    val subType = (
      absenceSubTypeCode?.let { rdProvider(AbsenceSubType::class, it) }
        ?: linkProvider(ABSENCE_SUB_TYPE, type)
      ) as? AbsenceSubType
    val reasonCategory = (
      absenceReasonCategoryCode?.let { rdProvider(AbsenceReasonCategory::class, it) }
      ) as AbsenceReasonCategory?
    val reason = (
      absenceReasonCode?.let { rdProvider(AbsenceReason::class, it) }
        ?: linkProvider(ABSENCE_REASON, reasonCategory ?: subType ?: type)
      ) as AbsenceReason
    return TemporaryAbsenceAuthorisation(
      person = person,
      prisonCode = prisonCode,
      absenceType = type,
      absenceSubType = subType,
      absenceReasonCategory = reasonCategory,
      absenceReason = reason,
      status = rdProvider(AuthorisationStatus::class, statusCode.name) as AuthorisationStatus,
      comments = comments,
      start = start,
      end = end,
      repeat = repeat,
      reasonPath = ReasonPath(reasonPath()),
      schedule = schedule,
      accompaniedBy = rdProvider(AccompaniedBy::class, accompaniedByCode) as AccompaniedBy,
      transport = rdProvider(Transport::class, transportCode) as Transport,
      locations = occurrences.mapTo(linkedSetOf()) { it.location },
      legacyId = null,
    )
  }

  private fun CreateTapAuthorisationRequest.OccurrenceRequest.asOccurrence(
    authorisation: TemporaryAbsenceAuthorisation,
    rdProvider: (KClass<out ReferenceData>, String) -> ReferenceData,
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
  ).calculateStatus { rdProvider(OccurrenceStatus::class, it) as OccurrenceStatus }

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
