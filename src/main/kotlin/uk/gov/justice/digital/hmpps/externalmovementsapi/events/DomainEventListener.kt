package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.sentry.Sentry
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.PrisonerMergedHandler
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.person.PersonUpdatedHandler
import uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write.AuthorisationApprovedHandler

@Service
class DomainEventListener(
  private val objectMapper: ObjectMapper,
  private val person: PersonUpdatedHandler,
  private val merged: PrisonerMergedHandler,
  private val authApproved: AuthorisationApprovedHandler,
) {
  @SqsListener("hmppsdomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun receive(notification: Notification) {
    try {
      when (notification.eventType) {
        PrisonerUpdated.EVENT_TYPE -> person.handle(objectMapper.readValue(notification.message))
        PrisonerMerged.EVENT_TYPE -> merged.handle(objectMapper.readValue(notification.message))
        TemporaryAbsenceAuthorisationApproved.EVENT_TYPE -> authApproved.handle(objectMapper.readValue(notification.message))
      }
    } catch (ex: Exception) {
      Sentry.captureException(ex)
      throw ex
    } finally {
      ExternalMovementContext.clear()
    }
  }
}
