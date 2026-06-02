package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.VersionSigner
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.PrisonTapLocations
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.PrisonTapLocationsRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.TapLocations

@Service
class TapLocationManager(
  private val tapLocations: PrisonTapLocationsRepository,
  private val versionSigner: VersionSigner,
) {
  fun getForPrison(prisonCode: String): TapLocations = tapLocations.findByIdOrNull(prisonCode)?.versioned()
    ?: TapLocations(versionSigner.generateToken(prisonCode, 0), linkedSetOf())

  fun setForPrison(prisonCode: String, request: TapLocations) {
    require(versionSigner.verifyToken(request.version, prisonCode)) { "Invalid version token" }
    tapLocations.save(PrisonTapLocations(prisonCode, request.version.number, request.locations.toList()))
  }

  private fun PrisonTapLocations.versioned(): TapLocations = TapLocations(versionSigner.generateToken(prisonCode, version), locations.mapTo(linkedSetOf()) { it })
}
