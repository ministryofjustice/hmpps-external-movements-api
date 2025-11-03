package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEventRepository

@TestConfiguration
class TestConfig(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val hmppsDomainEventRepository: HmppsDomainEventRepository,
  private val temporaryAbsenceAuthorisationRepository: TemporaryAbsenceAuthorisationRepository,
  private val temporaryAbsenceOccurrenceRepository: TemporaryAbsenceOccurrenceRepository,
  private val temporaryAbsenceMovementRepository: TemporaryAbsenceMovementRepository,
) {
  @Bean
  fun tempAbsenceAuthorisationOperations(): TempAbsenceAuthorisationOperations = TempAbsenceAuthorisationOperationsImpl(
    transactionTemplate,
    referenceDataRepository,
    temporaryAbsenceAuthorisationRepository,
  )

  @Bean
  fun tempAbsenceOccurrenceOperations(): TempAbsenceOccurrenceOperations = TempAbsenceOccurrenceOperationsImpl(
    transactionTemplate,
    referenceDataRepository,
    temporaryAbsenceOccurrenceRepository,
  )

  @Bean
  fun tempAbsenceMovementOperations(): TempAbsenceMovementOperations = TempAbsenceMovementOperationsImpl(
    transactionTemplate,
    referenceDataRepository,
    temporaryAbsenceMovementRepository,
  )

  @Bean
  fun hmppsDomainEventOperations(): HmppsDomainEventOperations = HmppsDomainEventOperationsImpl(transactionTemplate, hmppsDomainEventRepository)
}
