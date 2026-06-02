package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import java.util.UUID

data class ReferenceId(val id: UUID)
data class ReferenceIds(val ids: List<ReferenceId>) {
  val id = ids.first().id
}
