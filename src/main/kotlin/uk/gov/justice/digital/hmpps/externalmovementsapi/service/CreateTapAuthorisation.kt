package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisationRepository
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TAP_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataDomain.Code.TRANSPORT
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.TapStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.CreateTapAuthorisationRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ReferenceId

@Transactional
@Service
class CreateTapAuthorisation(
  private val prisonerSearch: PrisonerSearchClient,
  private val referenceDataRepository: ReferenceDataRepository,
  private val tapAuthRepository: TemporaryAbsenceAuthorisationRepository,
) {
  fun tapAuthorisation(personIdentifier: String, request: CreateTapAuthorisationRequest): ReferenceId {
    val prisoner = prisonerSearch.getPrisoner(personIdentifier) ?: throw NotFoundException("Prisoner not found")
    val rdMap =
      referenceDataRepository.findByKeyIn(request.requiredReferenceData().map { it.first of it.second }.toSet())
        .associateBy { it.key }
    val linkProvider = { id: Long -> referenceDataRepository.findLinkedItems(id).single() }
    val rdProvider = { dc: ReferenceDataDomain.Code, c: String -> requireNotNull(rdMap[dc of c]) }
    val authorisation = tapAuthRepository.findByPersonIdentifierAndReleaseAtAndReturnBy(
      personIdentifier,
      request.releaseAt,
      request.returnBy,
    )?.also {
      throw ConflictException("A matching TAP authorisation already exists")
    } ?: request.asEntity(personIdentifier, prisoner.lastPrisonId, rdProvider, linkProvider)
    return ReferenceId(tapAuthRepository.save(authorisation).id)
  }

  fun CreateTapAuthorisationRequest.asEntity(
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
      repeat = repeat,
      releaseAt = releaseAt,
      returnBy = returnBy,
      locationType = rdProvider(ReferenceDataDomain.Code.LOCATION_TYPE, locationTypeCode) as LocationType,
      accompanied = accompanied,
      accompaniedBy = accompaniedByCode?.let { rdProvider(ACCOMPANIED_BY, it) as AccompaniedBy },
      transport = transportCode?.let { rdProvider(TRANSPORT, it) as Transport },
      status = rdProvider(TAP_STATUS, statusCode) as TapStatus,
      notes = notes,
      applicationDate = applicationDate,
      submittedAt = submittedAt,
      submittedBy = submittedBy,
      approvedAt = approvedAt,
      approvedBy = approvedBy,
      locationId = locationId,
      legacyId = null,
      contact = null,
    )
  }
}
