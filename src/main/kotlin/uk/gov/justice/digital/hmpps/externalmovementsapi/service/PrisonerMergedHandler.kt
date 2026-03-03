package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.set
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.authorisationMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.occurrenceMatchesPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerMerged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerMergedInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.nomis.MigrationClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService

@Service
class PrisonerMergedHandler(
  private val transactionTemplate: TransactionTemplate,
  private val personSummaryService: PersonSummaryService,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
  private val migrationClient: MigrationClient,
) {
  fun handle(de: DomainEvent<PrisonerMergedInformation>) {
    val pmi = de.additionalInformation
    personSummaryService.findPersonSummary(pmi.removedNomsNumber)?.also { person ->
      ExternalMovementContext.get().copy(reason = PrisonerMerged.DESCRIPTION, source = DataSource.NOMIS).set()
      transactionTemplate.executeWithoutResult {
        val toPerson = personSummaryService.getWithSave(pmi.nomsNumber)
        authorisationRepository.findAll(authorisationMatchesPersonIdentifier(pmi.removedNomsNumber, null))
          .forEach { auth -> auth.moveTo(toPerson) }
        occurrenceRepository.findAll(occurrenceMatchesPersonIdentifier(pmi.removedNomsNumber, null))
          .forEach { occ -> occ.moveTo(toPerson) }
        movementRepository.findAllByPersonIdentifier(pmi.removedNomsNumber)
          .forEach { movement -> movement.moveTo(toPerson) }
        personSummaryService.remove(person)
      }
      migrationClient.requestRepair(pmi.removedNomsNumber)
    }
    migrationClient.requestRepair(pmi.nomsNumber)
  }
}
