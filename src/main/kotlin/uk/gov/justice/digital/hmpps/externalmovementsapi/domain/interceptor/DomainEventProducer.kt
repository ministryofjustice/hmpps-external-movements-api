package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent

interface DomainEventProducer {
  fun initialEvent(): DomainEvent<*>?

  @Deprecated("moving to build based on actions - so not required")
  fun stateChangedEvent(previousState: (String) -> Any?): DomainEvent<*>? = null

  fun domainEvents(): Set<DomainEvent<*>> = setOf()
}
