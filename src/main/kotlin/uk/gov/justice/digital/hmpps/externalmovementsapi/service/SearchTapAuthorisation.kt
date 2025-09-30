package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.authorisationMatchesDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.authorisationMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.authorisationMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.authorisationStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
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
    return page.map { it.with(getPerson(it.personIdentifier), occurrences[it.id] ?: emptyList()) }.asResponse()
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
    absenceType?.asCodedDescription(),
    absenceSubType?.asCodedDescription(),
    absenceReason?.asCodedDescription(),
    repeat,
    fromDate,
    toDate,
    occurrences.map { Location(it.locationType.asCodedDescription()) },
    occurrences.size,
    submittedAt,
  )
}

fun Page<TapAuthorisationResult>.asResponse() = TapAuthorisationSearchResponse(content, PageMetadata(totalElements))
