package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.getMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.forIntegration
import java.util.UUID

@Service
class IntegrationRetriever(
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun authorisation(id: UUID): IntegrationAuthorisation = authorisationRepository.getAuthorisation(id).forIntegration()

  fun occurrencesForAuthorisation(id: UUID): List<IntegrationOccurrence> = occurrenceRepository.findByAuthorisationId(id).sortedBy { it.start }.map { it.forIntegration() }

  fun occurrence(id: UUID): IntegrationOccurrence = occurrenceRepository.getOccurrence(id).forIntegration()

  fun movementsForOccurrence(id: UUID): List<IntegrationMovement> = movementRepository.findByOccurrenceId(id).sortedBy { it.occurredAt }.map { it.forIntegration() }

  fun movement(id: UUID): IntegrationMovement = movementRepository.getMovement(id).forIntegration()
}

private fun TemporaryAbsenceAuthorisation.forIntegration() = IntegrationAuthorisation(
  id,
  person.identifier,
  prisonCode,
  status.forIntegration(),
  IntegrationReason(absenceReason.code, absenceReason.description, hierarchyDescription(reasonPath)),
  transport.forIntegration(),
  accompaniedBy.forIntegration(),
  repeat,
  start,
  end,
  locations.toList(),
  comments,
)

private fun TemporaryAbsenceOccurrence.forIntegration() = IntegrationOccurrence(
  id,
  authorisation.id,
  person.identifier,
  prisonCode,
  status.forIntegration(),
  IntegrationReason(absenceReason.code, absenceReason.description, hierarchyDescription(reasonPath)),
  transport.forIntegration(),
  accompaniedBy.forIntegration(),
  start,
  end,
  location,
  comments,
)

private fun TemporaryAbsenceMovement.forIntegration() = IntegrationMovement(
  id,
  occurrence?.id,
  person.identifier,
  prisonCode,
  direction,
  IntegrationReason(absenceReason.code, absenceReason.description, null),
  accompaniedBy.forIntegration(),
  accompaniedByComments,
  occurredAt,
  location,
  comments,
)
