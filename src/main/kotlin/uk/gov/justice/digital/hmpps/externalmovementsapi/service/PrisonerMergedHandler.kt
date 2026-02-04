package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonSummaryService

@Transactional
@Service
class PrisonerMergedHandler(
  private val personSummaryService: PersonSummaryService,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val movementRepository: TemporaryAbsenceMovementRepository,
) {
  fun handle(de: DomainEvent<PrisonerMergedInformation>) {
    val pmi = de.additionalInformation
    personSummaryService.findPersonSummary(pmi.removedNomsNumber)?.also {
      ExternalMovementContext.get().copy(reason = PrisonerMerged.DESCRIPTION, source = DataSource.NOMIS).set()
      val toPerson = personSummaryService.getWithSave(pmi.nomsNumber)
      authorisationRepository.findAll(authorisationMatchesPersonIdentifier(pmi.removedNomsNumber))
        .forEach { auth -> auth.moveTo(toPerson) }
      occurrenceRepository.findAll(occurrenceMatchesPersonIdentifier(pmi.removedNomsNumber))
        .forEach { occ -> occ.moveTo(toPerson) }
      movementRepository.findAllByPersonIdentifier(pmi.removedNomsNumber)
        .forEach { movement -> movement.moveTo(toPerson) }
      personSummaryService.remove(it)
    }
  }
}
