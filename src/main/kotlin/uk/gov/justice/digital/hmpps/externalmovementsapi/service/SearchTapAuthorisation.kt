package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.authorisationMatchesDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.authorisationMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.authorisationMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.authorisationStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson

@Service
class SearchTapAuthorisation(
  private val prisonerSearch: PrisonerSearchClient,
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun find(request: TapAuthorisationSearchRequest): TapAuthorisationSearchResponse {
    val page = authRepository.findAll(request.asSpecification(), request.pageable())
    val occurrences = occurrenceRepository.findByAuthorisationIdIn(page.content.map { it.id }.toSet())
      .groupBy { it.authorisation.id }
    val prisoners = prisonerSearch.getPrisoners(page.map { it.personIdentifier }.toSet())
      .associateBy { it.prisonerNumber }
    val getPerson = { personIdentifier: String ->
      prisoners[personIdentifier]?.asPerson() ?: Person.unknown(personIdentifier)
    }
    return page.map { taa ->
      taa.with(getPerson(taa.personIdentifier), occurrences[taa.id] ?: emptyList())
    }.asResponse()
  }

  private fun TapAuthorisationSearchRequest.asSpecification(): Specification<TemporaryAbsenceAuthorisation> = listOfNotNull(
    authorisationMatchesPrisonCode(prisonCode),
    authorisationMatchesDateRange(fromDate, toDate),
    status.takeIf { it.isNotEmpty() }?.let { authorisationStatusCodeIn(it) },
    queryString?.let { authorisationMatchesPersonIdentifier(it) },
  ).reduce { spec, current -> spec.and(current) }

  private fun TemporaryAbsenceAuthorisation.with(
    person: Person,
    occurrences: List<TemporaryAbsenceOccurrence>,
  ): TapAuthorisationResult = TapAuthorisationResult(
    id = id,
    person = person,
    status.asCodedDescription(),
    absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
    absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
    absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }?.asCodedDescription(),
    absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
    repeat,
    fromDate,
    toDate,
    occurrences.map { it.location }.distinct(),
    occurrences.size,
    submittedAt,
  )
}

fun Page<TapAuthorisationResult>.asResponse() = TapAuthorisationSearchResponse(content, PageMetadata(totalElements))
