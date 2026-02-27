package uk.gov.justice.digital.hmpps.externalmovementsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration.ofSeconds

@Configuration
class WebClientConfiguration(
  @Value($$"${integration.hmpps-auth.url}") private val hmppsAuthBaseUri: String,
  @Value($$"${integration.manage-users.url}") private val manageUsersBaseUri: String,
  @Value($$"${integration.prison-register.url}") private val prisonRegisterBaseUri: String,
  @Value($$"${integration.prisoner-search.url}") private val prisonerSearchBaseUri: String,
) {
  private val timeout = ofSeconds(2)
  private val defaultRegistrationId = "default"

  @Bean
  fun hmppsAuthHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, timeout)

  @Bean
  fun manageUsersHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(manageUsersBaseUri, timeout)

  @Bean
  fun prisonerRegisterHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(prisonRegisterBaseUri, timeout)

  @Bean
  fun prisonerSearchHealthWebClient(builder: Builder): WebClient = builder.healthWebClient(prisonerSearchBaseUri, timeout)

  @Bean
  fun manageUsersWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = builder.authorisedWebClient(authorizedClientManager, defaultRegistrationId, manageUsersBaseUri, timeout)

  @Bean
  fun prisonRegisterApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder): WebClient = builder.authorisedWebClient(authorizedClientManager, defaultRegistrationId, prisonRegisterBaseUri, timeout)

  @Bean
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: Builder) = builder.authorisedWebClient(authorizedClientManager, defaultRegistrationId, prisonerSearchBaseUri, timeout)
}
