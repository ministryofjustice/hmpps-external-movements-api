package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service.search

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.mapping.asPerson
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationIsAccompanied
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationMatchesPersonName
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationMatchesPersonPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationOverlapsDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.authorisationStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.matchesAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapAuthorisationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.paged.TapAuthorisationSearchResponse

@Service
class SearchTapAuthorisation(
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun find(request: TapAuthorisationSearchRequest): TapAuthorisationSearchResponse {
    val page = authRepository.findAll(request.asSpecification(), request.pageable())
    val occurrences = occurrenceRepository.findByAuthorisationIdIn(page.map { it.id }.toSet())
      .groupBy { it.authorisation.id }
    return page.map { it.with(occurrences[it.id] ?: emptyList()) }.asResponse()
  }

  private fun TapAuthorisationSearchRequest.asSpecification(): Specification<TemporaryAbsenceAuthorisation> = listOfNotNull(
    authorisationMatchesPrisonCode(prisonCode),
    authorisationOverlapsDateRange(start, end),
    status.takeIf { it.isNotEmpty() }?.let { authorisationStatusCodeIn(it) },
    absenceCategorisation?.matchesAuthorisation(),
    isAccompanied?.let { authorisationIsAccompanied(it) },
    queryString?.let {
      if (isPersonIdentifier()) {
        authorisationMatchesPersonIdentifier(it, prisonCode)
      } else {
        authorisationMatchesPersonName(it, prisonCode)
      }
    } ?: authorisationMatchesPersonPrisonCode(prisonCode),
  ).reduce(Specification<TemporaryAbsenceAuthorisation>::and)

  private fun TemporaryAbsenceAuthorisation.with(
    occurrences: List<TemporaryAbsenceOccurrence>,
  ): TapAuthorisationResult = TapAuthorisationResult(
    id = id,
    person = person.asPerson(),
    status = status.asCodedDescription(),
    absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
    absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
    absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
      ?.asCodedDescription(),
    absenceReason = absenceReason.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
    accompaniedBy = accompaniedBy.asCodedDescription(),
    repeat = repeat,
    start = start,
    end = end,
    locations = locations.toList(),
    occurrenceCount = occurrences.size,
    absenceCategorisation = hierarchyDescription(reasonPath),
  )
}

fun Page<TapAuthorisationResult>.asResponse() = TapAuthorisationSearchResponse(content, PageMetadata(totalElements))
