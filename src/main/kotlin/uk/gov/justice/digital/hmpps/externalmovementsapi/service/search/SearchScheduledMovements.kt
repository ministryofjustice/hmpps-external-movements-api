package uk.gov.justice.digital.hmpps.externalmovementsapi.service.search

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesPrisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrencePersonIdentifierIn
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.startAfter
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.startBefore
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovement.Detail
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovement.Detail.Companion.buildUiUrl
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovementDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovementType
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ScheduledMovements
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.SearchScheduledMovementsRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class SearchScheduledMovements(
  private val serviceConfig: ServiceConfig,
  private val taoRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun search(prisonCode: String, request: SearchScheduledMovementsRequest): ScheduledMovements = taoRepository.findAll(request.asSpecification(prisonCode)).mapNotNull {
    it.scheduledMovement(request.includeSensitive, request.includeLocation, serviceConfig.uiBaseUrl)
  }.let {
    ScheduledMovements(it)
  }
}

private fun SearchScheduledMovementsRequest.asSpecification(prisonCode: String): Specification<TemporaryAbsenceOccurrence> = listOfNotNull(
  occurrenceMatchesPrisonCode(prisonCode),
  personIdentifiers.takeIf { it.isNotEmpty() }?.let { occurrencePersonIdentifierIn(it) },
  startAfter(start),
  startBefore(end),
).reduce(Specification<TemporaryAbsenceOccurrence>::and)

private fun TemporaryAbsenceOccurrence.scheduledMovement(
  includeSensitive: Boolean,
  includeLocation: Boolean,
  uiBaseUrl: String,
): ScheduledMovement? = if (includeSensitive || isNotSensitive()) {
  ScheduledMovement(
    id,
    person.identifier,
    ScheduledMovementDomain.EXTERNAL_MOVEMENTS.asCodedDescription(),
    ScheduledMovementType.TEMPORARY_ABSENCE.asCodedDescription(),
    hierarchyDescription(reasonPath),
    start,
    end,
    if (includeLocation) location.toString() else "External",
    status.asCodedDescription(),
    Detail(buildUiUrl(uiBaseUrl, id), setOf()),
  )
} else {
  null
}

private fun TemporaryAbsenceOccurrence.isToday() = LocalDate.now().isEqual(start.toLocalDate()) || LocalDate.now().isEqual(end.toLocalDate())

private fun TemporaryAbsenceOccurrence.isNotSensitive(): Boolean = isToday() ||
  accompaniedBy.code in listOf(
    AccompaniedBy.Code.ACCOMPANIED.value,
    AccompaniedBy.Code.UNACCOMPANIED.value,
  )
