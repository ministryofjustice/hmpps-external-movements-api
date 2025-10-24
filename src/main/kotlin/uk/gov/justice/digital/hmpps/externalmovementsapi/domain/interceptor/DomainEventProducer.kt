package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent

interface DomainEventProducer {
  fun initialEvent(): DomainEvent<*>?
  fun stateChangedEvent(previousState: (String) -> Any?): DomainEvent<*>?
}
