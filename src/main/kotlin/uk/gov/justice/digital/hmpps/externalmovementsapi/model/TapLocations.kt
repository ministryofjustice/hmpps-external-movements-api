package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.util.SequencedSet

data class TapLocations(
  val version: VersionToken,
  @Size(max = 250, message = "Cannot save more than 250 temporary absence locations")
  val locations: SequencedSet<Location>,
)
