package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.hibernate.envers.query.AuditEntity.revisionNumber
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.externalmovementsapi.audit.AuditRevision
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.DomainEventPublication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.event.producer.publication
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.Notification
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TestConfig
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.ManageUsersExtension
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.publish
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Duration
import java.time.Instant.now
import java.util.concurrent.TimeUnit

@ExtendWith(
  value = [
    HmppsAuthApiExtension::class,
    ManageUsersExtension::class,
    PrisonerSearchExtension::class,
  ],
)
@ContextConfiguration(classes = [TestConfig::class])
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
abstract class IntegrationTest {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var jsonMapper: JsonMapper

  @Autowired
  protected lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  protected lateinit var entityManager: EntityManager

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var referenceDataRepository: ReferenceDataRepository

  protected val hmppsEventsTestQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtesttopic")
      ?: throw MissingTopicException("hmppseventtesttopic not found")
  }

  protected fun sendDomainEvent(event: DomainEvent<*>) {
    domainEventsTopic.publish(event.eventType, jsonMapper.writeValueAsString(event))
  }

  protected fun HmppsQueue.receiveDomainEventsOnQueue(maxMessages: Int = 10): List<DomainEvent<*>> = sqsClient.receiveMessage(
    ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build(),
  ).get().messages()
    .map { jsonMapper.readValue<Notification>(it.body()) }
    .map { jsonMapper.readValue<DomainEvent<*>>(it.message) }

  protected fun setAuthorisation(
    username: String? = DEFAULT_USERNAME,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read", "write"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun verifyAudit(
    entity: Identifiable,
    revisionType: RevisionType,
    affectedEntities: Set<String>,
    context: ExternalMovementContext = ExternalMovementContext.get(),
  ) {
    transactionTemplate.execute {
      val auditReader = AuditReaderFactory.get(entityManager)
      assertTrue(auditReader.isEntityClassAudited(entity::class.java))

      val revisionNumber =
        auditReader
          .getRevisions(entity::class.java, entity.id)
          .filterIsInstance<Long>()
          .max()

      val entityRevision: Array<*> =
        auditReader
          .createQuery()
          .forRevisionsOfEntity(entity::class.java, false, true)
          .add(revisionNumber().eq(revisionNumber))
          .add(AuditEntity.id().eq(entity.id))
          .resultList
          .first() as Array<*>
      assertThat(entityRevision[2]).isEqualTo(revisionType)

      val auditRevision = entityRevision[1] as AuditRevision
      with(auditRevision) {
        assertThat(this.affectedEntities).containsExactlyInAnyOrderElementsOf(affectedEntities)
        assertThat(username).isEqualTo(context.username)
        assertThat(source).isEqualTo(context.source)
        assertThat(reason).isEqualTo(context.reason)
      }
    }
  }

  protected fun verifyEvents(
    entity: Identifiable,
    events: Set<DomainEvent<*>>,
  ) {
    verifyEventPublications(entity, events.map { it.publication(entity.id) }.toSet())
  }

  protected fun verifyEventPublications(
    entity: Identifiable,
    events: Set<DomainEventPublication>,
  ) {
    transactionTemplate.execute {
      val auditReader = AuditReaderFactory.get(entityManager)
      assertTrue(auditReader.isEntityClassAudited(entity::class.java))

      val revisionNumber =
        auditReader
          .getRevisions(entity::class.java, entity.id)
          .filterIsInstance<Long>()
          .max()

      val domainEventsPersisted: List<HmppsDomainEvent> =
        auditReader
          .createQuery()
          .forRevisionsOfEntity(HmppsDomainEvent::class.java, true, true)
          .add(revisionNumber().eq(revisionNumber))
          .resultList
          .filterIsInstance<HmppsDomainEvent>()
      domainEventsPersisted.forEach {
        assertThat(it.eventType).isEqualTo(it.event.eventType)
      }
      assertThat(domainEventsPersisted.mapNotNull { de -> de.entityId?.let { de.event.publication(it) { !de.published } } })
        .containsExactlyInAnyOrderElementsOf(events)
    }
  }

  protected fun waitUntil(
    pollingInterval: Duration = Duration.ofMillis(100),
    maxInterval: Duration = Duration.ofSeconds(1),
    predicate: () -> Boolean,
  ) {
    val end = now().plus(maxInterval)
    var result: Boolean
    do {
      TimeUnit.MILLISECONDS.sleep(pollingInterval.toMillis())
      result = predicate()
    } while (!result && now().isBefore(end))
    if (!result) {
      throw IllegalStateException("Predicate not met before timeout")
    }
  }

  protected final inline fun <reified T : Any> WebTestClient.ResponseSpec.successResponse(status: HttpStatus = HttpStatus.OK): T = expectStatus().isEqualTo(status)
    .expectBody<T>()
    .returnResult().responseBody!!

  protected final fun WebTestClient.ResponseSpec.errorResponse(status: HttpStatus): ErrorResponse = expectStatus().isEqualTo(status)
    .expectBody<ErrorResponse>()
    .returnResult().responseBody!!

  @BeforeEach
  fun clearContext() {
    ExternalMovementContext.clear()
  }

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance
    const val DEFAULT_USERNAME = "ExMoDeUs1"
    const val DEFAULT_NAME = "External Movement"

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
