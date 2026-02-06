package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEventRepository

@TestConfiguration
class TestConfig(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val hmppsDomainEventRepository: HmppsDomainEventRepository,
  private val temporaryAbsenceAuthorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val temporaryAbsenceOccurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val temporaryAbsenceMovementRepository: TemporaryAbsenceMovementRepository,
  private val personSummaryRepository: PersonSummaryRepository,
) {
  @Bean
  fun tempAbsenceAuthorisationOperations(personSummaryOperations: PersonSummaryOperations): TempAbsenceAuthorisationOperations = TempAbsenceAuthorisationOperationsImpl(
    transactionTemplate,
    referenceDataRepository,
    temporaryAbsenceAuthorisationRepository,
    personSummaryOperations,
  )

  @Bean
  fun tempAbsenceOccurrenceOperations(): TempAbsenceOccurrenceOperations = TempAbsenceOccurrenceOperationsImpl(
    transactionTemplate,
    referenceDataRepository,
    temporaryAbsenceOccurrenceRepository,
    temporaryAbsenceAuthorisationRepository,
  )

  @Bean
  fun tempAbsenceMovementOperations(personSummaryOperations: PersonSummaryOperations): TempAbsenceMovementOperations = TempAbsenceMovementOperationsImpl(
    transactionTemplate,
    referenceDataRepository,
    temporaryAbsenceMovementRepository,
    personSummaryOperations,
  )

  @Bean
  fun hmppsDomainEventOperations(): HmppsDomainEventOperations = HmppsDomainEventOperationsImpl(transactionTemplate, hmppsDomainEventRepository)

  @Bean
  fun personSummaryOperations(): PersonSummaryOperations = PersonSummaryOperationsImpl(personSummaryRepository)
}
