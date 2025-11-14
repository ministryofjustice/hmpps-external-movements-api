package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity.revisionNumber
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.support.TransactionTemplate
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.audit.AuditRevision
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.Identifiable
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
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

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
abstract class IntegrationTest {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  protected lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  protected lateinit var entityManager: EntityManager

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  internal val hmppsEventsTestQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw MissingTopicException("hmppseventtopic not found")
  }

  fun HmppsQueue.receiveDomainEventsOnQueue(maxMessages: Int = 10): List<DomainEvent<*>> = sqsClient.receiveMessage(
    ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build(),
  ).get().messages()
    .map { objectMapper.readValue<Notification>(it.body()) }
    .map { objectMapper.readValue<DomainEvent<*>>(it.message) }

  internal fun setAuthorisation(
    username: String? = DEFAULT_USERNAME,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read", "write"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  internal fun verifyAudit(
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

  fun verifyEvents(
    entity: Identifiable,
    events: Set<DomainEvent<*>>,
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
      assertThat(domainEventsPersisted.map { it.event }).containsExactlyInAnyOrderElementsOf(events)
    }
  }

  internal final inline fun <reified T> WebTestClient.ResponseSpec.successResponse(status: HttpStatus = HttpStatus.OK): T = expectStatus().isEqualTo(status)
    .expectBody(T::class.java)
    .returnResult().responseBody!!

  internal final fun WebTestClient.ResponseSpec.errorResponse(status: HttpStatus): ErrorResponse = expectStatus().isEqualTo(status)
    .expectBody(ErrorResponse::class.java)
    .returnResult().responseBody!!

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
