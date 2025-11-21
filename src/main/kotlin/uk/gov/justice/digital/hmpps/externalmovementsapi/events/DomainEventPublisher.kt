package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Pageable
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.sns.model.PublishBatchRequest
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import software.amazon.awssdk.services.sns.model.PublishBatchResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.hmpps.sqs.DEFAULT_BACKOFF_POLICY
import uk.gov.justice.hmpps.sqs.DEFAULT_RETRY_POLICY
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.eventTypeSnsMap

@Service
class DomainEventPublisher(
  private val objectMapper: ObjectMapper,
  private val hmppsQueueService: HmppsQueueService,
  private val domainEventRepository: HmppsDomainEventRepository,
) {
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw IllegalStateException("Domain event topic not found")
  }

  @Transactional
  fun publishUnpublishedEvents() {
    domainEventRepository.findByPublishedIsFalseOrderById(Pageable.ofSize(BATCH_SIZE))
      .takeIf { it.isNotEmpty() }
      ?.also { events -> domainEventsTopic.publishBatch(events.map { it.event }) }
      ?.forEach { it.published = true }
  }

  private fun HmppsTopic.publishBatch(
    events: List<DomainEvent<*>>,
    retryPolicy: RetryPolicy = DEFAULT_RETRY_POLICY,
    backOffPolicy: BackOffPolicy = DEFAULT_BACKOFF_POLICY,
  ) {
    val retryTemplate = RetryTemplate().apply {
      setRetryPolicy(retryPolicy)
      setBackOffPolicy(backOffPolicy)
    }
    val publishRequest = PublishBatchRequest.builder().topicArn(arn).publishBatchRequestEntries(
      events.map {
        PublishBatchRequestEntry.builder()
          .id(newUuid().toString())
          .message(objectMapper.writeValueAsString(it))
          .messageAttributes(eventTypeSnsMap(it.eventType, noTracing = true))
          .build()
      },
    ).build()
    retryTemplate.execute<PublishBatchResponse, RuntimeException> {
      snsClient.publishBatch(publishRequest).get()
    }
  }

  companion object {
    private const val BATCH_SIZE: Int = 10
  }
}
