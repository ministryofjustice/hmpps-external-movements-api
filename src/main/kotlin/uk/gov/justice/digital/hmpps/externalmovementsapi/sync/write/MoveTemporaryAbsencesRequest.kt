package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.write

import java.util.SequencedSet
import java.util.UUID

data class MoveTemporaryAbsencesRequest(
  val fromPersonIdentifier: String,
  val toPersonIdentifier: String,
  val authorisationIds: SequencedSet<UUID>,
  val unscheduledMovementIds: SequencedSet<UUID>,
)
