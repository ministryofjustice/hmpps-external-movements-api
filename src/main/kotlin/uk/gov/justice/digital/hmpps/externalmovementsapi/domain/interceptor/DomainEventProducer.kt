package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent

interface DomainEventProducer : Identifiable {
  fun initialEvent(): DomainEventPublication?

  fun domainEvents(): Set<DomainEventPublication> = setOf()
}

data class DomainEventPublication(val event: DomainEvent<*>, val publish: Boolean = true)

fun DomainEvent<*>.publication(publishSupplier: (DomainEvent<*>) -> Boolean = { true }) = DomainEventPublication(this, publishSupplier(this))
