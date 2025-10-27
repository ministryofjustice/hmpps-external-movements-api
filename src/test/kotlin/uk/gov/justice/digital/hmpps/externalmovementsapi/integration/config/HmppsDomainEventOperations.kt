package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEventRepository
import java.util.UUID

interface HmppsDomainEventOperations {
  fun givenHmppsDomainEvent(event: DomainEvent<*>): HmppsDomainEvent
  fun findHmppsDomainEvent(id: UUID): HmppsDomainEvent?
  fun markAllAsPublished()
}

class HmppsDomainEventOperationsImpl(
  private val transactionTemplate: TransactionTemplate,
  private val hmppsDomainEventRepository: HmppsDomainEventRepository,
) : HmppsDomainEventOperations {
  override fun givenHmppsDomainEvent(event: DomainEvent<*>): HmppsDomainEvent = hmppsDomainEventRepository.save(HmppsDomainEvent(event))

  override fun findHmppsDomainEvent(id: UUID): HmppsDomainEvent? = hmppsDomainEventRepository.findByIdOrNull(id)
  override fun markAllAsPublished() = transactionTemplate.executeWithoutResult {
    hmppsDomainEventRepository.findAll().forEach { it.apply { published = true } }
  }
}
