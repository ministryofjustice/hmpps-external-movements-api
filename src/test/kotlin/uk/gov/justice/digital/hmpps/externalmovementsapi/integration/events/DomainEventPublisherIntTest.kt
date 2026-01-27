package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEventPublisher
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationApproved
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.HmppsDomainEventOperations

class DomainEventPublisherIntTest(
  @Autowired private val dep: DomainEventPublisher,
  @Autowired private val hmppsDomainEventOperations: HmppsDomainEventOperations,
) : IntegrationTest(),
  HmppsDomainEventOperations by hmppsDomainEventOperations {
  @Test
  fun `can publish unpublished domain event`() {
    markAllAsPublished()

    val id = newUuid()
    val personIdentifier = personIdentifier()
    val toPublish = givenHmppsDomainEvent(TemporaryAbsenceAuthorisationApproved(personIdentifier, id), id)

    dep.publishUnpublishedEvents()

    val domainEvent = requireNotNull(findHmppsDomainEvent(toPublish.id))
    assertThat(domainEvent.published).isTrue()

    val published = hmppsEventsTestQueue.receiveDomainEventsOnQueue()
    assertThat(published.contains(toPublish.event))
  }
}
