package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TapAuthorisation(
  val id: UUID,
  val person: Person,
  val status: CodedDescription,
  val absenceType: CodedDescription?,
  val absenceSubType: CodedDescription?,
  val absenceReason: CodedDescription?,
  val repeat: Boolean,
  val fromDate: LocalDate,
  val toDate: LocalDate,
  val occurrences: List<TapOccurrence>,
  val submitted: AtAndBy,
  val approved: AtAndBy?,
)

data class TapOccurrence(
  val id: UUID,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val location: Location,
  val accompaniedBy: CodedDescription,
  val transport: CodedDescription,
)

data class AtAndBy(val at: LocalDateTime, val by: String)
