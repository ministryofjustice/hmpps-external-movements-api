package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.client.UserDetails

class ManageUsersServer : WireMockServer(9000) {
  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  fun userDetails(userDetails: UserDetails) {
    stubFor(
      get(urlEqualTo("/users/${userDetails.username}"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userDetails))
            .withStatus(200),
        ),
    )
  }
}

class ManageUsersExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsers = ManageUsersServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsers.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsers.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsers.stop()
  }
}
