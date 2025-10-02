package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.organisations

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.retryOnTransientException
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged.LocationSearchRequest

@Component
class OrganisationsClient(@Qualifier("organisationsWebClient") private val webClient: WebClient) {
  fun findOrganisations(request: LocationSearchRequest): PagedOrganisations = webClient
    .get()
    .uri {
      it.path(ORGANISATION_SEARCH_URL)
      it.queryParam("name", request.queryString)
      it.queryParam("page", request.page)
      it.queryParam("size", request.size)
      it.queryParam("sort", "organisationName")
      it.build()
    }
    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
    .exchangeToMono { res ->
      when (res.statusCode()) {
        HttpStatus.OK -> res.bodyToMono<PagedOrganisations>()
        else -> res.createError()
      }
    }.retryOnTransientException()
    .block()!!

  companion object {
    const val ORGANISATION_SEARCH_URL = "organisation/search"
  }
}
