package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription
import java.time.LocalDateTime
import java.util.UUID

data class TapOccurrence(
  val id: UUID,
  val authorisation: Authorisation,
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  val location: Location,
  val accompaniedBy: CodedDescription,
  val transport: CodedDescription,
  val added: AtAndBy,
  val cancelled: AtAndBy?,
) {
  data class Authorisation(
    val id: UUID,
    val person: Person,
    val status: CodedDescription,
    val absenceType: CodedDescription?,
    val absenceSubType: CodedDescription?,
    val absenceReason: CodedDescription?,
  )
}
