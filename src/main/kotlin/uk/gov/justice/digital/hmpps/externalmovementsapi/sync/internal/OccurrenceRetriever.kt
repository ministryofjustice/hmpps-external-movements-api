package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.AtAndBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.TapOccurrence
import java.util.UUID

@Transactional(readOnly = true)
@Service
class OccurrenceRetriever(
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun getById(id: UUID): TapOccurrence = occurrenceRepository.getOccurrence(id).forSync()
}

private fun TemporaryAbsenceOccurrence.forSync() = TapOccurrence(
  id = id,
  authorisation = authorisation.forOccurrence(),
  statusCode = requireNotNull(status).code,
  releaseAt = releaseAt,
  returnBy = returnBy,
  location = location,
  accompaniedByCode = accompaniedBy.code,
  transportCode = transport.code,
  notes = notes,
)

private fun TemporaryAbsenceAuthorisation.forOccurrence() = TapOccurrence.Authorisation(
  id = id,
  statusCode = status.code,
  absenceTypeCode = absenceType?.code,
  absenceSubTypeCode = absenceSubType?.code,
  absenceReasonCode = requireNotNull(absenceReason).code,
  repeat = repeat,
  submitted = AtAndBy(submittedAt, submittedBy),
)
