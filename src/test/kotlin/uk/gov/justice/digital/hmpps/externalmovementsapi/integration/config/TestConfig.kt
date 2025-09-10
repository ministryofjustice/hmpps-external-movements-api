package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.TemporaryAbsenceSeriesRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceSeriesOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.TempAbsenceSeriesOperationsImpl

@TestConfiguration
class TestConfig(
  private val transactionTemplate: TransactionTemplate,
  private val referenceDataRepository: ReferenceDataRepository,
  private val temporaryAbsenceSeriesRepository: TemporaryAbsenceSeriesRepository,
) {
  @Bean
  fun tempAbsenceSeriesOperations(): TempAbsenceSeriesOperations = TempAbsenceSeriesOperationsImpl(transactionTemplate, referenceDataRepository, temporaryAbsenceSeriesRepository)
}
