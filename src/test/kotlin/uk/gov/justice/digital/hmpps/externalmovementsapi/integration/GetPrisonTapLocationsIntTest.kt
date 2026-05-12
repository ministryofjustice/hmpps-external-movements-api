package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.VersionSigner

class GetPrisonTapLocationsIntTest(
  @Autowired private val versionSigner: VersionSigner,
) : IntegrationTest() {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .get()
      .uri(PRISON_TAP_LOCATIONS_URL, prisonCode())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW, Roles.EXTERNAL_MOVEMENTS_RO, Roles.EXTERNAL_MOVEMENTS_RW])
  fun `403 forbidden without correct role`(role: String) {
    getPrisonTapLocations(prisonCode(), role).expectStatus().isForbidden
  }

  @Test
  fun `200 ok - empty locations when no tap locations exist for given prison`() {
    val prisonCode = prisonCode()
    val res = getPrisonTapLocations(prisonCode).successResponse<TapLocations>(HttpStatus.OK)
    assertThat(res.version).isEqualTo(versionSigner.generateToken(prisonCode, 0))
    assertThat(res.locations).isEmpty()
  }

  @Test
  fun `200 ok - prison tap locations returned`() {
    val ptl = givenPrisonTapLocations()
    val res = getPrisonTapLocations(ptl.prisonCode).successResponse<TapLocations>(HttpStatus.OK)
    assertThat(res.version).isEqualTo(versionSigner.generateToken(ptl.prisonCode, ptl.version))
    assertThat(res.locations.first).isEqualTo(ptl.locations.first)
    assertThat(res.locations.last).isEqualTo(ptl.locations.last)
  }

  private fun getPrisonTapLocations(
    prisonCode: String,
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .get()
    .uri(PRISON_TAP_LOCATIONS_URL, prisonCode)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val PRISON_TAP_LOCATIONS_URL = "/prisons/{prisonCode}/temporary-absence-locations"
  }
}
