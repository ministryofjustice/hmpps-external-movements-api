package uk.gov.justice.digital.hmpps.externalmovementsapi.service.search

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationMatchesPersonName
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationOverlapsDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.matchesAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson

@Service
class SearchTapAuthorisation(
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun find(request: TapAuthorisationSearchRequest): TapAuthorisationSearchResponse {
    val page = authRepository.findAll(request.asSpecification(), request.pageable())
    val occurrences = occurrenceRepository.findByAuthorisationIdIn(page.content.map { it.id }.toSet())
      .groupBy { it.authorisation.id }
    return page.map { taa ->
      taa.with(taa.person.asPerson(), occurrences[taa.id] ?: emptyList())
    }.asResponse()
  }

  private fun TapAuthorisationSearchRequest.asSpecification(): Specification<TemporaryAbsenceAuthorisation> = listOfNotNull(
    authorisationMatchesPrisonCode(prisonCode),
    authorisationOverlapsDateRange(start, end),
    status.takeIf { it.isNotEmpty() }?.let { authorisationStatusCodeIn(it) },
    absenceCategorisation?.matchesAuthorisation(),
    queryString?.let {
      if (isPersonIdentifier()) {
        authorisationMatchesPersonIdentifier(it)
      } else {
        authorisationMatchesPersonName(it)
      }
    },
  ).reduce(Specification<TemporaryAbsenceAuthorisation>::and)

  private fun TemporaryAbsenceAuthorisation.with(
    person: Person,
    occurrences: List<TemporaryAbsenceOccurrence>,
  ): TapAuthorisationResult = TapAuthorisationResult(
    id = id,
    person = person,
    status = status.asCodedDescription(),
    absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
    absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
    absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
      ?.asCodedDescription(),
    absenceReason = absenceReason.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
    repeat = repeat,
    start = start,
    end = end,
    locations = occurrences.map { it.location }.distinct(),
    occurrenceCount = occurrences.size,
    absenceCategorisation = hierarchyDescription(reasonPath),
  )
}

fun Page<TapAuthorisationResult>.asResponse() = TapAuthorisationSearchResponse(content, PageMetadata(totalElements))
