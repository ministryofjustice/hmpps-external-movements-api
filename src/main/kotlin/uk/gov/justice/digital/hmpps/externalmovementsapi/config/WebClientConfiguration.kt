package uk.gov.justice.digital.hmpps.externalmovementsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration.ofSeconds

@Configuration
class WebClientConfiguration(
  @Value($$"${hmpps-auth.url}") private val hmppsAuthBaseUri: String,
  @Value($$"${manage-users.url}") private val manageUsersBaseUri: String,
) {
  private val timeout = ofSeconds(2)

  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, timeout)

  @Bean
  fun manageUsersHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(manageUsersBaseUri, timeout)

  @Bean
  fun manageUsersWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder.authorisedWebClient(authorizedClientManager, "manage-users-api", manageUsersBaseUri, timeout)
}
