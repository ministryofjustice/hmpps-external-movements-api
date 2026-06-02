package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.write

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.referencedata.OccurrenceStatusRepository

@Transactional
@Service
class AuthorisationApprovedHandler(
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceStatusRepository: OccurrenceStatusRepository,
  private val occRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun handle(de: DomainEvent<TemporaryAbsenceAuthorisationInformation>) {
    de.takeIf { it.additionalInformation.source == DataSource.NOMIS }
      ?.let { authRepository.findByIdOrNull(de.additionalInformation.id) }
      ?.takeIf { it.repeat }?.also { auth ->
        val rd = occurrenceStatusRepository.findAll().associateBy { it.code }
        occRepository.findByAuthorisationId(auth.id).forEach {
          it.calculateStatus { statusCode -> rd[statusCode] as OccurrenceStatus }
        }
      }
  }
}
