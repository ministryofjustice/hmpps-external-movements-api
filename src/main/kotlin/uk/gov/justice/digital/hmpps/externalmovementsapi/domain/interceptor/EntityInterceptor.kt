package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import jakarta.persistence.EntityManager
import org.hibernate.Interceptor
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent

@Component
class EntityInterceptor : Interceptor {
  private lateinit var em: EntityManager

  @Autowired
  fun setDomainEventRepository(@Lazy entityManager: EntityManager) {
    em = entityManager
  }

  override fun onFlushDirty(
    entity: Any,
    id: Any,
    currentState: Array<out Any>,
    previousState: Array<out Any>,
    propertyNames: Array<out String>,
    types: Array<out Type>,
  ): Boolean {
    if (entity is DomainEventProducer && !ExternalMovementContext.get().migratingData) {
      entity.domainEvents().forEach {
        em.persist(
          HmppsDomainEvent(it, entity.id).apply { published = it.eventType in entity.excludeFromPublish() },
        )
      }
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
    if (entity is DomainEventProducer && !ExternalMovementContext.get().migratingData) {
      entity.initialEvent()?.let { em.persist(HmppsDomainEvent(it, entity.id)) }
    }
    return super.onPersist(entity, id, state, propertyNames, types)
  }
}
