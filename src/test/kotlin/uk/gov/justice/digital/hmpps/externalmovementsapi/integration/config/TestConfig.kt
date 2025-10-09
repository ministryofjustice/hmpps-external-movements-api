package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceMovementRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceAuthorisationOperationsImpl
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceMovementOperationsImpl
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceOccurrenceOperationsImpl

@TestConfiguration
class TestConfig(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
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
}
