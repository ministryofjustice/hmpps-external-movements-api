package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.search

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.Prison
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.mapping.asPerson
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.matchesOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceIsAccompanied
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceMatchesPersonName
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceMatchesPersonPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceOverlapsDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.occurrenceStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.PersonOccurrenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.PersonOccurrenceResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.PersonTapSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.PersonTapSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapOccurrenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapOccurrenceResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapOccurrenceSearchResponse

@Service
class SearchTapOccurrence(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val prisonRegister: PrisonRegisterClient,
) {
  fun find(request: TapOccurrenceSearchRequest): TapOccurrenceSearchResponse {
    val page = occurrenceRepository.findAll(request.asSpecification(), request.pageable())
    return page.map { it.toModel() }.asResponse()
  }

  fun findForPerson(personIdentifier: String, request: PersonTapSearchRequest): PersonTapSearchResponse {
    val page = occurrenceRepository.findAll(request.specificationFor(personIdentifier), request.pageable())
    val prisons = prisonRegister.findPrisons(page.map { it.prisonCode }.toSet()).associateBy { it.code }
    return page.map { it.forPerson { code -> prisons[code] ?: Prison.default(code) } }.asResponse()
  }

  private fun TapOccurrenceSearchRequest.asSpecification(): Specification<TemporaryAbsenceOccurrence> = listOfNotNull(
    occurrenceMatchesPrisonCode(prisonCode),
    occurrenceOverlapsDateRange(start, end),
    status.takeIf { it.isNotEmpty() }?.let { occurrenceStatusCodeIn(it) },
    absenceCategorisation?.matchesOccurrence(),
    isAccompanied?.let { occurrenceIsAccompanied(it) },
    queryString?.let {
      if (isPersonIdentifier()) {
        occurrenceMatchesPersonIdentifier(it, prisonCode)
      } else {
        occurrenceMatchesPersonName(it, prisonCode)
      }
    } ?: occurrenceMatchesPersonPrisonCode(prisonCode),
  ).reduce(Specification<TemporaryAbsenceOccurrence>::and)

  private fun PersonTapSearchRequest.specificationFor(personIdentifier: String): Specification<TemporaryAbsenceOccurrence> = listOfNotNull(
    occurrenceMatchesPersonIdentifier(personIdentifier, null),
    occurrenceOverlapsDateRange(start, end),
    isAccompanied?.let { occurrenceIsAccompanied(it) },
    status.takeIf { it.isNotEmpty() }?.let { occurrenceStatusCodeIn(it) },
    absenceCategorisation?.matchesOccurrence(),
  ).reduce(Specification<TemporaryAbsenceOccurrence>::and)

  private fun TemporaryAbsenceOccurrence.toModel() = TapOccurrenceResult(
    id = id,
    authorisation = authorisation.asOccurrenceAuth(),
    status = status.asCodedDescription(),
    absenceType = absenceType?.asCodedDescription(),
    absenceSubType = absenceSubType?.asCodedDescription(),
    absenceReasonCategory = absenceReasonCategory?.asCodedDescription(),
    absenceReason = absenceReason.asCodedDescription(),
    start = start,
    end = end,
    accompaniedBy = accompaniedBy.asCodedDescription(),
    transport = transport.asCodedDescription(),
    location = location,
    isCancelled = status.code == OccurrenceStatus.Code.CANCELLED.name,
    absenceCategorisation = hierarchyDescription(reasonPath),
  )
}

private fun TemporaryAbsenceAuthorisation.asOccurrenceAuth() = TapOccurrenceAuthorisation(
  id,
  person.asPerson(),
  status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  repeat = repeat,
  schedule = schedule,
)

fun Page<TapOccurrenceResult>.asResponse() = TapOccurrenceSearchResponse(content, PageMetadata(totalElements))

private fun TemporaryAbsenceOccurrence.forPerson(prisonSupplier: (String) -> Prison) = PersonOccurrenceResult(
  id = id,
  authorisation = authorisation.forPerson(),
  prison = prisonSupplier(prisonCode),
  status = status.asCodedDescription(),
  absenceType = absenceType?.asCodedDescription(),
  absenceSubType = absenceSubType?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.asCodedDescription(),
  absenceReason = absenceReason.asCodedDescription(),
  start = start,
  end = end,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  transport = transport.asCodedDescription(),
  location = location,
  isCancelled = status.code == OccurrenceStatus.Code.CANCELLED.name,
  absenceCategorisation = hierarchyDescription(reasonPath),
)

private fun TemporaryAbsenceAuthorisation.forPerson() = PersonOccurrenceAuthorisation(
  id,
  status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  repeat = repeat,
)

fun Page<PersonOccurrenceResult>.asResponse() = PersonTapSearchResponse(content, PageMetadata(totalElements))
