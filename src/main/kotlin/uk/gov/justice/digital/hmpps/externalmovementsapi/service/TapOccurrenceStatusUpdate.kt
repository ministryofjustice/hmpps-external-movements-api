package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.data.domain.PageRequest.ofSize
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatusRepository
import java.time.Duration
import java.time.LocalDateTime.now

@Transactional
@Service
class OccurrenceStatusUpdate(
  private val occurrenceStatusRepository: OccurrenceStatusRepository,
  private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository,
) {
  fun pastOccurrencesOfInterest() {
    val rd = occurrenceStatusRepository.findAll().associateBy { it.code }
    occurrenceRepository.findPastOccurrences(statusCodes, now(), ofSize(100)).forEach {
      it.calculateStatus { statusCode -> rd[statusCode] as OccurrenceStatus }
    }
  }

  companion object {
    private val statusCodes = setOf(
      OccurrenceStatus.Code.PENDING.name,
      OccurrenceStatus.Code.SCHEDULED.name,
      OccurrenceStatus.Code.IN_PROGRESS.name,
    )
  }
}

@Conditional(PollPastOccurrencesCondition::class)
@Service
class PastOccurrencesPoller(private val update: OccurrenceStatusUpdate) {
  @Scheduled(fixedDelayString = $$"${service.occurrence-status.poll-interval}")
  fun recalculatePastOccurrences() = try {
    update.pastOccurrencesOfInterest()
  } finally {
    ExternalMovementContext.clear()
  }
}

class PollPastOccurrencesCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<Duration>("service.occurrence-status.poll-interval", Duration.ZERO).isPositive
}
