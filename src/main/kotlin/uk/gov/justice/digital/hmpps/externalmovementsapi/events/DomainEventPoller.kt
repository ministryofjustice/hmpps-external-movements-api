package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import io.sentry.Sentry
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import java.time.Duration

@Conditional(PollDomainEventsCondition::class)
@Service
class DomainEventPoller(private val domainEventPublisher: DomainEventPublisher) {
  @Scheduled(fixedDelayString = $$"${service.domain-events.poll-interval}")
  fun publishUnpublishedEvents() {
    try {
      domainEventPublisher.publishUnpublishedEvents()
    } catch (e: Exception) {
      Sentry.captureException(e)
    } finally {
      ExternalMovementContext.clear()
    }
  }
}

class PollDomainEventsCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<Duration>("service.domain-events.poll-interval", Duration.ZERO).isPositive
}
