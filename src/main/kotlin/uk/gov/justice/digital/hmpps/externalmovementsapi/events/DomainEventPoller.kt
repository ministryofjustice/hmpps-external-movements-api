package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Conditional(PollIntervalCondition::class)
@Service
class DomainEventPoller(private val domainEventPublisher: DomainEventPublisher) {
  @Scheduled(fixedDelayString = $$"${service.domain-events.poll-interval}")
  fun publishUnpublishedEvents() {
    domainEventPublisher.publishUnpublishedEvents()
  }
}

class PollIntervalCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<Int>("service.domain-events.poll-interval", 0) > 0
}
