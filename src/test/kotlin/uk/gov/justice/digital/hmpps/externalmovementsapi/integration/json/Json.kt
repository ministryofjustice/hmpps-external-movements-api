package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.json

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jsonMapper

object Json {
  @JvmStatic
  val mapper: JsonMapper = jsonMapper()
}
