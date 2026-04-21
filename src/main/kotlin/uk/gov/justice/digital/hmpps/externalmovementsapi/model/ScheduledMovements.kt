package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class ScheduledMovements(
  val content: List<ScheduledMovement>,
)

data class ScheduledMovement(
  val id: UUID,
  val personIdentifier: String,
  val domain: CodedDescription,
  val type: CodedDescription,
  val description: ScheduledMovementDescription,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: LocationDescription,
  val status: CodedDescription,
  val detail: ScheduledMovementDetail,
)

interface NamedAndDescribed {
  val name: String
  val description: String
}

enum class ScheduledMovementDomain(override val description: String) : NamedAndDescribed {
  EXTERNAL_MOVEMENTS("External Movements"),
}

enum class ScheduledMovementType(override val description: String) : NamedAndDescribed {
  TEMPORARY_ABSENCE("Temporary Absence"),
}

fun NamedAndDescribed.asCodedDescription() = CodedDescription(name, description)
