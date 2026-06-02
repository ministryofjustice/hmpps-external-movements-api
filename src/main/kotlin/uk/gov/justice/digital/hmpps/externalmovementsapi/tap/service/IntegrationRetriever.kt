package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.getAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.movement.getMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.getOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.IntegrationReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.integration.forIntegration
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
