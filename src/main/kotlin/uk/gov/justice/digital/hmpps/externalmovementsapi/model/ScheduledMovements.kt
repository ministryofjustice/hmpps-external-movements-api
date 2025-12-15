package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.SequencedSet
import java.util.UUID

data class ScheduledMovements(
  val content: List<ScheduledMovement>,
)

data class ScheduledMovement(
  val id: UUID,
  val personIdentifier: String,
  val domain: CodedDescription,
  val type: CodedDescription,
  val description: String,
  val start: LocalDateTime,
  val end: LocalDateTime,
  val location: String,
  val status: CodedDescription,
  val detail: Detail,
) {
  data class Detail(
    val uiUrl: String,
    val requiredRoles: Set<String>,
  ) {
    companion object {
      fun buildUiUrl(uiBaseUrl: String, occurrenceId: UUID): String = "$uiBaseUrl/temporary-absences/$occurrenceId"
    }
  }
}

@ValidStartAndEnd
data class SearchScheduledMovementsRequest(
  val personIdentifiers: SequencedSet<String> = linkedSetOf(),
  override val start: LocalDateTime = LocalDate.now().atStartOfDay(),
  override val end: LocalDateTime = LocalDate.now().plusDays(1).atStartOfDay(),
  val includeSensitive: Boolean = false,
  val includeLocation: Boolean = false,
) : StartAndEnd<LocalDateTime>

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
