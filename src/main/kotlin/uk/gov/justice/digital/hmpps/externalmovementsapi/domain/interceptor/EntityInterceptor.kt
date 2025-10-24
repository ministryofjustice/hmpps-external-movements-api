package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import org.hibernate.Interceptor
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEventRepository

@Component
class EntityInterceptor : Interceptor {
  private lateinit var domainEvents: HmppsDomainEventRepository

  @Autowired
  fun setDomainEventRepository(@Lazy domainEvents: HmppsDomainEventRepository) {
    this.domainEvents = domainEvents
  }

  override fun onFlushDirty(
    entity: Any,
    id: Any,
    currentState: Array<out Any>,
    previousState: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    val getPreviousState = { name: String -> previousState[propertyNames.indexOf(name)] }
    if (entity is DomainEventProducer) {
      entity.stateChangedEvent(getPreviousState)?.let { domainEvents.save(HmppsDomainEvent(it)) }
    }
    return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types)
  }

  override fun onPersist(
    entity: Any,
    id: Any,
    state: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    if (entity is DomainEventProducer) {
      entity.initialEvent()?.let { domainEvents.save(HmppsDomainEvent(it)) }
    }
    return super.onPersist(entity, id, state, propertyNames, types)
  }
}
