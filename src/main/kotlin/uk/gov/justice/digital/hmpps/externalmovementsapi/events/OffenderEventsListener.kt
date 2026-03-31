package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.ServiceConfig
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.ExternalMovementHandler
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.ExternalMovementRecordedEvent

@Component
class OffenderEventsListener(
  private val serviceConfig: ServiceConfig,
  private val jsonMapper: JsonMapper,
  private val emHandler: ExternalMovementHandler,
) {
  @SqsListener("offendereventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    if (!serviceConfig.autoCancelEnabled) return
    try {
      when (notification.eventType) {
        ExternalMovementRecordedEvent.EVENT_TYPE ->
          emHandler.handle(jsonMapper.readValue<ExternalMovementRecordedEvent>(notification.message))
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      ExternalMovementContext.clear()
    }
  }
}
