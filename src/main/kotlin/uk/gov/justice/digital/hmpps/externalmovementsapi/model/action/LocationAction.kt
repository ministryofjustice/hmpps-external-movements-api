package uk.gov.justice.digital.hmpps.externalmovementsapi.model.action

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

interface LocationAction {
  val location: Location
  val ignoreTruncation: Boolean

  infix fun changes(original: Location): Boolean = location.address != original.address || location.postcode != original.postcode || location.description changes original.description

  private infix fun String?.changes(original: String?): Boolean = when (ignoreTruncation) {
    false -> this != original
    true -> {
      if (this == original) return false
      if (original == null || this == null) return true
      original.length < 40 || !original.startsWith(this)
    }
  }
}
