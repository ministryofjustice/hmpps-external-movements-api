package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonUpdatedHandler

@Service
class DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val person: PersonUpdatedHandler,
) {
  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    when (notification.eventType) {
      PrisonerUpdated.EVENT_TYPE -> person.handle(objectMapper.readValue(notification.message))
      /*
      TODO: handle other event types
      "prison-offender-events.prisoner.merged"
      "prison-offender-events.prisoner.booking.moved"
       */
    }
  }
}
