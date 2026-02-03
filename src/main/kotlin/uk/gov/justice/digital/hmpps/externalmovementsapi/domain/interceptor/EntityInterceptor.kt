package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import jakarta.persistence.EntityManager
import org.hibernate.Interceptor
import org.hibernate.Transaction
import org.hibernate.type.Type
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.DomainEventProducer
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import java.util.UUID

@Component
class EntityInterceptor : Interceptor {
  private lateinit var em: EntityManager
  private val publishedEventKeys = ThreadLocal.withInitial { mutableSetOf<Pair<UUID, String>>() }

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
        if (registerDomainEvent(it.entityId, it.event.eventType)) {
          em.persist(HmppsDomainEvent(it.event, it.entityId).apply { published = !it.publish })
        }
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
      entity.initialEvent()?.takeIf { registerDomainEvent(it.entityId, it.event.eventType) }?.let {
        em.persist(HmppsDomainEvent(it.event, it.entityId).apply { published = !it.publish })
      }
    }
    return super.onPersist(entity, id, state, propertyNames, types)
  }

  private fun registerDomainEvent(entityId: UUID, eventType: String): Boolean = publishedEventKeys.get().add(entityId to eventType)

  override fun afterTransactionCompletion(tx: Transaction) {
    publishedEventKeys.get().clear()
    super.afterTransactionCompletion(tx)
  }
}
