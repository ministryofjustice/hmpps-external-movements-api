package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.ACCOMPANIED_BY
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapAuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapOccurrenceRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId

@Transactional
@Service
class CreateTapAuthorisation(
  private val prisonerSearch: PrisonerSearchClient,
  private val referenceDataRepository: ReferenceDataRepository,
  private val tapAuthRepository: TemporaryAbsenceAuthorisationRepository,
  private val tapOccurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun tapAuthorisation(personIdentifier: String, request: CreateTapAuthorisationRequest): ReferenceId {
    val prisoner = prisonerSearch.getPrisoner(personIdentifier) ?: throw NotFoundException("Prisoner not found")
    val rdMap =
      referenceDataRepository.findByKeyIn(request.requiredReferenceData().map { it.first of it.second }.toSet())
        .associateBy { it.key }
    val linkProvider = { id: Long -> referenceDataRepository.findLinkedItems(id).single() }
    val rdProvider = { dc: ReferenceDataDomain.Code, c: String -> requireNotNull(rdMap[dc of c]) }
    request.occurrences.mapNotNull {
      tapOccurrenceRepository.findByPersonIdentifierAndReleaseAtAndReturnBy(
        personIdentifier,
        it.releaseAt,
        it.returnBy,
      )
    }.takeIf { it.isNotEmpty() }
      ?.also {
        throw ConflictException("A matching TAP authorisation already exists")
      }
    val authorisation = tapAuthRepository.save(
      request.asAuthorisation(personIdentifier, prisoner.lastPrisonId, rdProvider, linkProvider),
    )
    tapOccurrenceRepository.saveAll(
      request.occurrences.map { it.asOccurrence(authorisation, rdProvider) },
    )
    return ReferenceId(authorisation.id)
  }

  fun CreateTapAuthorisationRequest.asAuthorisation(
    personIdentifier: String,
    prisonCode: String,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
    linkProvider: (Long) -> ReferenceData,
  ): TemporaryAbsenceAuthorisation {
    val type = rdProvider(ABSENCE_TYPE, absenceTypeCode) as AbsenceType
    val subType = (
      absenceSubTypeCode?.let { rdProvider(ABSENCE_SUB_TYPE, it) }
        ?: linkProvider(type.id)
      ) as AbsenceSubType
    val reason = (
      absenceReasonCode?.let { rdProvider(ABSENCE_REASON, it) }
        ?: linkProvider(subType.id)
      ) as AbsenceReason
    return TemporaryAbsenceAuthorisation(
      personIdentifier = personIdentifier,
      prisonCode = prisonCode,
      absenceType = type,
      absenceSubType = subType,
      absenceReason = reason,
      status = rdProvider(TAP_AUTHORISATION_STATUS, statusCode.name) as TapAuthorisationStatus,
      notes = notes,
      applicationDate = applicationDate,
      submittedAt = submittedAt,
      submittedBy = submittedBy,
      approvedAt = approvedAt,
      approvedBy = approvedBy,
      repeat = occurrences.size > 1,
      legacyId = null,
    )
  }

  fun CreateTapOccurrenceRequest.asOccurrence(
    authorisation: TemporaryAbsenceAuthorisation,
    rdProvider: (ReferenceDataDomain.Code, String) -> ReferenceData,
  ): TemporaryAbsenceOccurrence {
    val authStatus = TapAuthorisationStatus.Code.valueOf(authorisation.status.code)
    val occurrenceStatusCode = when (authStatus) {
      TapAuthorisationStatus.Code.PENDING -> TapOccurrenceStatus.Code.PENDING
      TapAuthorisationStatus.Code.APPROVED -> TapOccurrenceStatus.Code.SCHEDULED
    }.name
    return TemporaryAbsenceOccurrence(
      authorisation,
      releaseAt = releaseAt,
      returnBy = returnBy,
      status = rdProvider(ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS, occurrenceStatusCode) as TapOccurrenceStatus,
      locationType = rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, locationTypeCode) as LocationType,
      accompaniedBy = accompaniedByCode?.let { rdProvider(ACCOMPANIED_BY, it) as AccompaniedBy },
      transport = transportCode?.let { rdProvider(TRANSPORT, it) as Transport },
      locationId = locationId,
      contact = null,
      addedAt = authorisation.submittedAt,
      addedBy = authorisation.submittedBy,
      cancelledAt = null,
      cancelledBy = null,
      notes = notes,
      legacyId = null,
    )
  }
}
