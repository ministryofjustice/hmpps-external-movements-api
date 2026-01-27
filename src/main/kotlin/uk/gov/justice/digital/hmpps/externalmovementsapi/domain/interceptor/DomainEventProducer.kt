package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent

interface DomainEventProducer : Identifiable {
  fun initialEvent(): DomainEvent<*>?

  fun domainEvents(): Set<DomainEvent<*>> = setOf()

  fun excludeFromPublish(): Set<String> = setOf()
}
