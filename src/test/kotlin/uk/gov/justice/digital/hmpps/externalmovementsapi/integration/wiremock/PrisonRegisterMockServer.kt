package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.Prison
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonregister.PrisonsByIdsRequest

class PrisonRegisterMockServer : WireMockServer(9005) {
  fun getPrison(prison: Prison) {
    getPrisons(setOf(prison))
  }

  fun getPrisons(
    prisons: Set<Prison>,
    prisonCodes: Set<String> = setOf(),
  ) {
    val request = PrisonsByIdsRequest((prisonCodes.takeIf { it.isNotEmpty() } ?: prisons.map { it.code }.toSet()))
    stubFor(
      WireMock
        .post(WireMock.urlPathEqualTo("/prisons/prisonsByIds"))
        .withRequestBody(
          equalToJson(
            jsonMapper().writeValueAsString(request),
            true,
            true,
          ),
        ).willReturn(
          WireMock
            .aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(
              jsonMapper().writeValueAsString(
                prisons.map {
                  mapOf(
                    "prisonId" to it.code,
                    "prisonName" to it.name,
                  )
                },
              ),
            ),
        ),
    )
  }

  companion object {
    fun prison() = Prison(prisonCode(), word(10))
  }
}

class PrisonerRegisterExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val prisonRegister = PrisonRegisterMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonRegister.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonRegister.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonRegister.stop()
  }
}
