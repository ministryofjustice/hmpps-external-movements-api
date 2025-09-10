package uk.gov.justice.digital.hmpps.externalmovementsapi.entity

import com.fasterxml.uuid.Generators
import java.util.UUID

object IdGenerator {
  fun newUuid(): UUID = Generators.timeBasedEpochGenerator().generate()
}
