package uk.gov.justice.digital.hmpps.externalmovementsapi.service.search

import org.springframework.data.domain.Page
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesDateRange
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesPersonName
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceStatusCodeIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceResult
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.TapOccurrenceSearchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson

@Service
class SearchTapOccurrence(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun find(request: TapOccurrenceSearchRequest): TapOccurrenceSearchResponse {
    val page = occurrenceRepository.findAll(request.asSpecification(), request.pageable())
    return page.map { it.toModel() }.asResponse()
  }

  private fun TapOccurrenceSearchRequest.asSpecification(): Specification<TemporaryAbsenceOccurrence> = listOfNotNull(
    occurrenceMatchesPrisonCode(prisonCode),
    occurrenceMatchesDateRange(start, end),
    status.takeIf { it.isNotEmpty() }?.let { occurrenceStatusCodeIn(it) },
    queryString?.let {
      if (it.matches(Prisoner.PATTERN.toRegex())) {
        occurrenceMatchesPersonIdentifier(it)
      } else {
        occurrenceMatchesPersonName(it)
      }
    },
  ).reduce(Specification<TemporaryAbsenceOccurrence>::and)

  private fun TemporaryAbsenceOccurrence.toModel() = TapOccurrenceResult(
    id = id,
    authorisation = authorisation.asOccurrenceAuth(),
    status = status.asCodedDescription(),
    absenceType = absenceType?.asCodedDescription(),
    absenceSubType = absenceSubType?.asCodedDescription(),
    absenceReasonCategory = absenceReasonCategory?.asCodedDescription(),
    absenceReason = absenceReason?.asCodedDescription(),
    start = start,
    end = end,
    accompaniedBy = accompaniedBy.asCodedDescription(),
    transport = transport.asCodedDescription(),
    location = location,
    status.code == OccurrenceStatus.Code.CANCELLED.name,
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
  absenceReason = absenceReason?.takeIf { reasonPath.has(ABSENCE_REASON) }?.asCodedDescription(),
  repeat = repeat,
)

fun Page<TapOccurrenceResult>.asResponse() = TapOccurrenceSearchResponse(content, PageMetadata(totalElements))
