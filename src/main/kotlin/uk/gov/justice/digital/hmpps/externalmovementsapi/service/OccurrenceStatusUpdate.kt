package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_OCCURRENCE_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapOccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import java.time.Duration

@Transactional
@Service
class OccurrenceStatusUpdate(
  private val referenceDataRepository: ReferenceDataRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun pastOccurrencesOfInterest() {
    val rd = referenceDataRepository.findByKeyDomainAndActiveTrue(TAP_OCCURRENCE_STATUS).associateBy { it.key.code }
    occurrenceRepository.findPastOccurrences(statusKeys, PageRequest.ofSize(100)).forEach {
      it.calculateStatus { statusCode -> rd[statusCode] as TapOccurrenceStatus }
    }
  }

  companion object {
    private val statusKeys = setOf(
      TAP_OCCURRENCE_STATUS of TapOccurrenceStatus.Code.SCHEDULED.name,
      TAP_OCCURRENCE_STATUS of TapOccurrenceStatus.Code.IN_PROGRESS.name,
    )
  }
}

@Conditional(PollPastOccurrencesCondition::class)
@Service
class PastOccurrencesPoller(private val update: OccurrenceStatusUpdate) {
  @Scheduled(fixedDelayString = $$"${service.occurrence-status.poll-interval}")
  fun recalculatePastOccurrences() {
    update.pastOccurrencesOfInterest()
  }
}

class PollPastOccurrencesCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<Duration>("service.domain-events.poll-interval", Duration.ZERO).isPositive
}
