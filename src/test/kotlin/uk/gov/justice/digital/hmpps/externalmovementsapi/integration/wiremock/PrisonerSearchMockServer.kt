package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.cellLocation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.dob
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.name
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.json.Json
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.PrisonerNumbers
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoners
import java.time.LocalDate

class PrisonerSearchServer : WireMockServer(9000) {

  fun getPrisoners(
    prisonCode: String,
    prisonNumbers: Set<String>,
    prisoners: List<Prisoner> = prisonNumbers.map { prisoner(prisonCode, it) },
  ) {
    stubFor(
      post(urlPathEqualTo("/prisoner-search/prisoner-numbers"))
        .withRequestBody(equalToJson(Json.mapper.writeValueAsString(PrisonerNumbers(prisonNumbers)), true, true))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(Json.mapper.writeValueAsString(prisoners))
            .withStatus(200),
        ),
    )
  }

  fun filteredPrisoners(
    prisonCode: String,
    prisoners: Prisoners,
    queryParams: Map<String, String> = mapOf(),
  ) {
    val request = get(urlPathEqualTo("/prison/$prisonCode/prisoners"))
    queryParams.forEach { queryParam ->
      request.withQueryParam(queryParam.key, equalTo(queryParam.value))
    }
    stubFor(
      request.willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(Json.mapper.writeValueAsString(prisoners))
          .withStatus(200),
      ),
    )
  }

  companion object {
    fun prisoner(
      prisonCode: String,
      personIdentifier: String = personIdentifier(),
      firstName: String = name(8),
      lastName: String = name(12),
      dateOfBirth: LocalDate = dob(),
      cellLocation: String = cellLocation(),
    ): Prisoner = Prisoner(
      personIdentifier,
      firstName,
      lastName,
      dateOfBirth,
      prisonCode,
      prisonCode,
      cellLocation,
    )
  }
}

class PrisonerSearchExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonerSearch = PrisonerSearchServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonerSearch.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonerSearch.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonerSearch.stop()
  }
}
