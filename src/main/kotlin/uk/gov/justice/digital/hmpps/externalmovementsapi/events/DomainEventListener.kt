package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.PrisonerMergedHandler
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonUpdatedHandler

@Service
class DomainEventListener(
  private val jsonMapper: JsonMapper,
  private val person: PersonUpdatedHandler,
  private val merged: PrisonerMergedHandler,
) {
  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    try {
      when (notification.eventType) {
        PrisonerUpdated.EVENT_TYPE -> person.handle(jsonMapper.readValue(notification.message))
        PrisonerMerged.EVENT_TYPE -> merged.handle(jsonMapper.readValue(notification.message))
      }
    } finally {
      ExternalMovementContext.clear()
    }
  }
}
