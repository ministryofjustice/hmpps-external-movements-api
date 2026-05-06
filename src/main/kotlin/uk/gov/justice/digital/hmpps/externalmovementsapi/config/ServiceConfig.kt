package uk.gov.justice.digital.hmpps.externalmovementsapi.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import reactor.netty.http.HttpProtocol
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration.IntegrationUrlBuilder
import java.time.Duration

@Component
class ServiceConfigInfo(
  private val serviceConfig: ServiceConfig,
) : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder.withDetail("activeAgencies", serviceConfig.activePrisons)
  }
}

@ConfigurationProperties(prefix = "service")
data class ServiceConfig(
  val activePrisons: Set<String>,
  val domainEvents: DomainEventConfig,
  val occurrenceStatus: OccurrenceStatusConfig,
  val uiBaseUrl: String,
  val apiBaseUrl: String,
  val disablePrisonEvents: Set<String> = setOf(),
  val autoCancelEnabled: Boolean = false,
  val httpProtocol: HttpProtocol,
) {
  data class DomainEventConfig(val pollInterval: Duration, val batchSize: Int)
  data class OccurrenceStatusConfig(val pollInterval: Duration)
}

@Component
class ServiceConfigLoader(private val serviceConfig: ServiceConfig) {
  @PostConstruct
  fun init() {
    IntegrationUrlBuilder.baseUrl = serviceConfig.apiBaseUrl
  }
}
