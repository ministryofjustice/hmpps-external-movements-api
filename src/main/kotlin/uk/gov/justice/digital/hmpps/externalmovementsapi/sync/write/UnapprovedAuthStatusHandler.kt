package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

@Transactional
@Service
class UnapprovedAuthStatusHandler(
  private val authRepository: TemporaryAbsenceAuthorisationRepository,
  private val occurrenceStatusRepository: OccurrenceStatusRepository,
  private val occRepository: TemporaryAbsenceOccurrenceRepository,
  private val objectMapper: ObjectMapper,
) {
  fun handle(de: DomainEvent<TemporaryAbsenceAuthorisationInformation>) {
    de.takeIf { it.additionalInformation.source == DataSource.NOMIS }
      ?.let { authRepository.findByIdOrNull(de.additionalInformation.id) }
      // unapproved events trigger this but should be ignored if the authorisation is approved at the time of processing
      ?.takeIf { !it.repeat && it.status.code != AuthorisationStatus.Code.APPROVED.name }?.also { auth ->
        val rd = occurrenceStatusRepository.findAll().associateBy(OccurrenceStatus::code)
        occRepository.findByAuthorisationId(auth.id).singleOrNull()?.let {
          if (it.status.code == OccurrenceStatus.Code.SCHEDULED.name && it.movements().isEmpty()) {
            occRepository.delete(it)
            null
          } else {
            it
          }
        } ?: occRepository.save(auth.occurrence().calculateStatus { statusCode -> rd[statusCode] as OccurrenceStatus })
      }
  }

  fun TemporaryAbsenceAuthorisation.occurrence(): TemporaryAbsenceOccurrence {
    val schedule = objectMapper.treeToValue<AuthorisationSchedule>(checkNotNull(schedule) { "Missing schedule for $id" })
    return TemporaryAbsenceOccurrence(
      authorisation = this,
      absenceType = absenceType,
      absenceSubType = absenceSubType,
      absenceReasonCategory = absenceReasonCategory,
      absenceReason = absenceReason,
      start = start.atTime(schedule.startTime),
      end = end.atTime(schedule.returnTime),
      contactInformation = null,
      accompaniedBy = accompaniedBy,
      transport = transport,
      location = locations.firstOrNull() ?: Location.empty(),
      comments = comments,
      legacyId = legacyId,
      reasonPath = reasonPath,
      scheduleReference = null,
    )
  }
}
