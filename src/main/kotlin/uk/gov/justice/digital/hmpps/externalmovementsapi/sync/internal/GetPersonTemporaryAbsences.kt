package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.internal

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.MovementInOutCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonAuthorisationCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonMovementsCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonOccurrenceCount
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonTapCounts
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read.PersonTapDetail

@Service
class GetPersonTemporaryAbsences(
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun count(personIdentifier: String): PersonTapCounts {
    val authCount = authorisationRepository.countByPersonIdentifier(personIdentifier)
    val occCount = occurrenceRepository.countByPersonIdentifier(personIdentifier)
    val movementSummary = movementRepository.summaryForPerson(personIdentifier)
    return PersonTapCounts(
      PersonAuthorisationCount(authCount),
      PersonOccurrenceCount(occCount),
      PersonMovementsCount(
        MovementInOutCount(movementSummary.schOut, movementSummary.schIn),
        MovementInOutCount(movementSummary.adocOut, movementSummary.adhocIn),
      ),
    )
  }

  fun detail(personIdentifier: String): PersonTapDetail {
    val occurrences =
      occurrenceRepository.findByPersonIdentifier(personIdentifier).groupBy { it.authorisation }
    val unscheduledKey = newUuid()
    val movements =
      movementRepository.findAllByPersonIdentifier(personIdentifier).groupBy { it.occurrence?.id ?: unscheduledKey }
    val authorisations = occurrences.map { e ->
      PersonTapDetail.Authorisation(
        e.key.id,
        AuthorisationStatus.Code.valueOf(e.key.status.code),
        e.key.prisonCode,
        e.value.map { occ ->
          PersonTapDetail.Occurrence(
            occ.id,
            OccurrenceStatus.Code.valueOf(occ.status.code),
            occ.prisonCode,
            movements[occ.id]?.map { m -> PersonTapDetail.Movement(m.id, m.direction, m.prisonCode) } ?: emptyList(),
          )
        },
      )
    }
    val unscheduled = movements[unscheduledKey]?.map { PersonTapDetail.Movement(it.id, it.direction, it.prisonCode) } ?: emptyList()
    return PersonTapDetail(
      authorisations,
      unscheduled,
    )
  }
}
