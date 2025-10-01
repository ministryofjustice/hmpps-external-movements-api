package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.manageusers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.retryOnTransientException

@Component
class ManageUsersClient(@Qualifier("manageUsersWebClient") private val webClient: WebClient) {

  fun getUsersDetails(usernames: Set<String>): List<UserDetails> = if (usernames.isEmpty()) {
    emptyList()
  } else {
    Flux
      .fromIterable(usernames)
      .flatMap({ findUserDetails(it) }, 10)
      .collectList()
      .block()!!
  }

  fun findUserDetails(username: String): Mono<UserDetails> = if (username == SYSTEM_USERNAME) {
    Mono.just(SYSTEM_USERNAME.asSystemUser())
  } else {
    webClient
      .get()
      .uri("/users/{username}", username)
      .exchangeToMono { res ->
        when (res.statusCode()) {
          HttpStatus.NOT_FOUND -> Mono.just(username.asSystemUser())
          HttpStatus.OK -> res.bodyToMono<UserDetails>()
          else -> res.createError()
        }
      }.retryOnTransientException()
  }
}
