package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.occurrenceMatchesDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.occurrenceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.occurrenceMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.Person
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson

@Service
class SearchTapOccurrence(
  private val prisonerSearch: PrisonerSearchClient,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun find(request: TapOccurrenceSearchRequest): TapOccurrenceSearchResponse {
    val page = occurrenceRepository.findAll(request.asSpecification(), request.pageable())
    val prisoners = prisonerSearch.getPrisoners(page.map { it.authorisation.personIdentifier }.toSet())
      .associateBy { it.prisonerNumber }
    val getPerson = { personIdentifier: String ->
      prisoners[personIdentifier]?.asPerson() ?: Person.unknown(personIdentifier)
    }
    return page.map { it.with(getPerson(it.authorisation.personIdentifier)) }.asResponse()
  }

  private fun TapOccurrenceSearchRequest.asSpecification(): Specification<TemporaryAbsenceOccurrence> = listOfNotNull(
    occurrenceMatchesPrisonCode(prisonCode),
    occurrenceMatchesDateRange(fromDate, toDate),
    queryString?.let { occurrenceMatchesPersonIdentifier(it) },
  ).reduce { spec, current -> spec.and(current) }

  private fun TemporaryAbsenceOccurrence.with(person: Person) = TapOccurrenceResult(
    id = id,
    authorisation = authorisation.asOccurrenceAuth(person),
    status = status.asCodedDescription(),
    absenceType = absenceType?.asCodedDescription(),
    absenceSubType = absenceSubType?.asCodedDescription(),
    absenceReasonCategory = absenceReasonCategory?.asCodedDescription(),
    absenceReason = absenceReason?.asCodedDescription(),
    releaseAt = releaseAt,
    returnBy = returnBy,
    accompaniedBy = accompaniedBy.asCodedDescription(),
    transport = transport.asCodedDescription(),
    location = location,
    status.code == TapOccurrenceStatus.Code.CANCELLED.name,
  )
}

private fun TemporaryAbsenceAuthorisation.asOccurrenceAuth(person: Person) = TapOccurrenceAuthorisation(
  id,
  person,
  status.asCodedDescription(),
  absenceType = absenceType?.takeIf { reasonPath.has(ABSENCE_TYPE) }?.asCodedDescription(),
  absenceSubType = absenceSubType?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  repeat = repeat,
)

fun Page<TapOccurrenceResult>.asResponse() = TapOccurrenceSearchResponse(content, PageMetadata(totalElements))
