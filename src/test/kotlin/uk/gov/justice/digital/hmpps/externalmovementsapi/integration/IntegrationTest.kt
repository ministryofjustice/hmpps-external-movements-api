package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TestConfig
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(value = [HmppsAuthApiExtension::class, PrisonerSearchExtension::class])
@ContextConfiguration(classes = [TestConfig::class])
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTest {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  internal fun setAuthorisation(
    username: String? = DEFAULT_USERNAME,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read", "write"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  internal val hmppsEventsTestQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw MissingTopicException("hmppseventtopic not found")
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
