package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.IdGenerator.newUuid
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.postcode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.word
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location

object LocationGenerator {
  fun location(
    description: String? = word(10),
    address: String? = word(16),
    postcode: String? = postcode(),
    uprn: Long? = newUuid().mostSignificantBits,
  ) = Location(description, address, postcode, uprn)
}
