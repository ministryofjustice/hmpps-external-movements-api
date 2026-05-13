package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.PrisonTapLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.LocationGenerator.location
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.TapLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.VersionToken
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.VersionSigner
import java.util.SequencedSet

class SetPrisonTapLocationsIntTest(
  @Autowired private val versionSigner: VersionSigner,
) : IntegrationTest() {
  @Test
  fun `401 unauthorised without a valid token`() {
    webTestClient
      .put()
      .uri(PRISON_TAP_LOCATIONS_URL, prisonCode())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = [Roles.TEMPORARY_ABSENCE_RO, Roles.TEMPORARY_ABSENCE_RW, Roles.EXTERNAL_MOVEMENTS_RO, Roles.EXTERNAL_MOVEMENTS_RW])
  fun `403 forbidden without correct role`(role: String) {
    setPrisonTapLocations(prisonCode(), role = role).expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid version format`() {
    webTestClient
      .put()
      .uri(PRISON_TAP_LOCATIONS_URL, prisonCode())
      .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(Roles.EXTERNAL_MOVEMENTS_UI)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(
        """
        {
          "version": "1.abc234.5",
          "locations": []
        }
        """.trimIndent(),
      )
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `400 bad request - invalid version`() {
    val request = TapLocations(VersionToken(23, "invalid-signature"), linkedSetOf(location()))
    val res = setPrisonTapLocations(prisonCode(), request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.userMessage).isEqualTo("Invalid request")
  }

  @Test
  fun `400 bad request - attempt to save over 250 locations`() {
    val locations: SequencedSet<Location> = (1..251).mapTo(linkedSetOf()) { location() }
    val prisonCode = prisonCode()
    val version = versionSigner.generateToken(prisonCode, 0)
    val request = TapLocations(version, locations)

    val res = setPrisonTapLocations(prisonCode, request).errorResponse(HttpStatus.BAD_REQUEST)
    assertThat(res.developerMessage).isEqualTo("Validation failure: Cannot save more than 250 temporary absence locations")
  }

  @Test
  fun `409 conflict - locations change by another user`() {
    val ptl = givenPrisonTapLocations()
    val version = versionSigner.generateToken(ptl.prisonCode, ptl.version)
    prisonTapLocationsRepository.save(PrisonTapLocations(ptl.prisonCode, ptl.version, linkedSetOf(location())))

    val res = setPrisonTapLocations(ptl.prisonCode, TapLocations(version, linkedSetOf(location())))
      .errorResponse(HttpStatus.CONFLICT)

    assertThat(res.userMessage).isEqualTo("Data integrity conflict")
    assertThat(res.developerMessage).startsWith("ObjectOptimisticLockingFailureException:")
  }

  @Test
  fun `204 no content - create prison tap locations`() {
    val prisonCode = prisonCode()
    val version = versionSigner.generateToken(prisonCode, 0)

    val request = TapLocations(version, linkedSetOf(location()))
    setPrisonTapLocations(prisonCode, request).expectStatus().isNoContent

    val saved = requireNotNull(prisonTapLocationsRepository.findByIdOrNull(prisonCode))
    assertThat(saved.locations).containsExactlyElementsOf(request.locations)
    assertThat(saved.version).isEqualTo(request.version.number)
  }

  @Test
  fun `204 no content - update prison tap locations`() {
    val ptl = givenPrisonTapLocations()
    val version = versionSigner.generateToken(ptl.prisonCode, ptl.version)

    val request = TapLocations(version, linkedSetOf(location(), ptl.locations.first))
    setPrisonTapLocations(ptl.prisonCode, request).expectStatus().isNoContent

    val saved = requireNotNull(prisonTapLocationsRepository.findByIdOrNull(ptl.prisonCode))
    assertThat(saved.locations).containsExactlyElementsOf(request.locations)
    assertThat(saved.version).isEqualTo(request.version.number + 1)
  }

  @Test
  fun `204 no content - remove all prison tap locations`() {
    val ptl = givenPrisonTapLocations()
    val version = versionSigner.generateToken(ptl.prisonCode, ptl.version)

    val request = TapLocations(version, linkedSetOf())
    setPrisonTapLocations(ptl.prisonCode, request).expectStatus().isNoContent

    val saved = requireNotNull(prisonTapLocationsRepository.findByIdOrNull(ptl.prisonCode))
    assertThat(saved.locations).containsExactlyElementsOf(request.locations)
    assertThat(saved.version).isEqualTo(request.version.number + 1)
  }

  private fun setPrisonTapLocations(
    prisonCode: String,
    request: TapLocations = TapLocations(versionSigner.generateToken(prisonCode, 0), linkedSetOf()),
    role: String? = Roles.EXTERNAL_MOVEMENTS_UI,
  ) = webTestClient
    .put()
    .uri(PRISON_TAP_LOCATIONS_URL, prisonCode)
    .bodyValue(request)
    .headers(setAuthorisation(username = SYSTEM_USERNAME, roles = listOfNotNull(role)))
    .exchange()

  companion object {
    const val PRISON_TAP_LOCATIONS_URL = "/prisons/{prisonCode}/temporary-absence-locations"
  }
}
