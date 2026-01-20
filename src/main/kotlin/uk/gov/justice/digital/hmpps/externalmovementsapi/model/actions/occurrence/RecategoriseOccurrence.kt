package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.AbsenceCategorisationAction

data class RecategoriseOccurrence(
  override val absenceTypeCode: String?,
  override val absenceSubTypeCode: String?,
  override val absenceReasonCategoryCode: String?,
  override val absenceReasonCode: String?,
  @JsonIgnore
  override val reasonPath: ReasonPath = ReasonPath(
    absenceTypeCode,
    absenceSubTypeCode,
    absenceReasonCategoryCode,
    absenceReasonCode,
  ),
  override val reason: String? = null,
) : OccurrenceAction,
  AbsenceCategorisationAction {
  constructor(aca: AbsenceCategorisationAction) : this(
    aca.absenceTypeCode,
    aca.absenceSubTypeCode,
    aca.absenceReasonCategoryCode,
    aca.absenceReasonCode,
    aca.reasonPath,
    aca.reason,
  )

  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceRecategorised(tao.person.identifier, tao.id)
}
