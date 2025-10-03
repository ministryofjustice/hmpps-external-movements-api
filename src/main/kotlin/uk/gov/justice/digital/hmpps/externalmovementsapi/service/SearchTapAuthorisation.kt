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
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.LocationType.Code.valueOf
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapAuthorisationSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.locationsearch.GetLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
class SearchTapAuthorisation(
  private val prisonerSearch: PrisonerSearchClient,
  private val locations: GetLocations,
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun find(request: TapAuthorisationSearchRequest): TapAuthorisationSearchResponse {
    val page = authRepository.findAll(request.asSpecification(), request.pageable())
    val occurrences = occurrenceRepository.findByAuthorisationIdIn(page.content.map { it.id }.toSet())
      .groupBy { it.authorisation.id }
    val locations =
      occurrences.values.flatMap { l -> l.map { valueOf(it.locationType.code) to it.locationId } }.let { ids ->
        locations.withIds(ids.groupBy({ it.first }, { it.second }))
      }.associateBy { it.type.code to it.id.toString() }
    val prisoners = prisonerSearch.getPrisoners(page.map { it.personIdentifier }.toSet())
      .associateBy { it.prisonerNumber }
    val getPerson = { personIdentifier: String ->
      prisoners[personIdentifier]?.asPerson() ?: Person.unknown(personIdentifier)
    }
    val getLocations = { id: UUID ->
      occurrences[id]?.mapNotNull { locations[it.locationType.code to it.locationId] } ?: emptyList()
    }
    return page.map { taa ->
      taa.with(getPerson(taa.personIdentifier), occurrences[taa.id] ?: emptyList()) { getLocations(it) }
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
    locations: (UUID) -> List<Location>,
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
    locations(id),
    occurrences.size,
    submittedAt,
  )
}

fun Page<TapAuthorisationResult>.asResponse() = TapAuthorisationSearchResponse(content, PageMetadata(totalElements))
