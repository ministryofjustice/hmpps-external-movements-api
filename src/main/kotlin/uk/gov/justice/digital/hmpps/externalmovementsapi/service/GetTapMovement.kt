package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_SUB_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.ABSENCE_TYPE
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.getMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.asCodedDescription
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.mapping.asPerson
import java.util.UUID

@Service
@Transactional(readOnly = true)
class GetTapMovement(
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun byId(id: UUID): TapMovement = movementRepository.getMovement(id).let(TemporaryAbsenceMovement::toModel)
}

private fun TemporaryAbsenceMovement.toModel() = TapMovement(
  id = id,
  person = person.asPerson(),
  occurrence = occurrence?.toModel(),
  occurredAt = occurredAt,
  direction = direction,
  prisonCode = prisonCode,
  absenceReason = absenceReason.asCodedDescription(),
  location = location,
  accompaniedBy = accompaniedBy.asCodedDescription(),
  accompaniedByComments = accompaniedByComments,
  comments = comments,
)

private fun TemporaryAbsenceOccurrence.toModel() = TapMovement.Occurrence(
  id = id,
  status = status.asCodedDescription(),
  absenceType = absenceType
    ?.takeIf { reasonPath.has(ABSENCE_TYPE) }
    ?.asCodedDescription(),
  absenceSubType = absenceSubType
    ?.takeIf { reasonPath.has(ABSENCE_SUB_TYPE) }
    ?.asCodedDescription(),
  absenceReasonCategory = absenceReasonCategory
    ?.takeIf { reasonPath.has(ABSENCE_REASON_CATEGORY) }
    ?.asCodedDescription(),
  absenceReason = absenceReason
    ?.takeIf { reasonPath.has(ABSENCE_REASON) }
    ?.asCodedDescription(),
  start = start,
  end = end,
)
