package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import java.util.UUID

interface DomainEventProducer : Identifiable {
  fun initialEvents(): Set<DomainEventPublication>

  fun domainEvents(): Set<DomainEventPublication> = setOf()
}

data class DomainEventPublication(val event: DomainEvent<*>, val entityId: UUID, val publish: Boolean = true)

fun DomainEvent<*>.publication(entityId: UUID, publishSupplier: (DomainEvent<*>) -> Boolean = { true }) = DomainEventPublication(this, entityId, publishSupplier(this))
