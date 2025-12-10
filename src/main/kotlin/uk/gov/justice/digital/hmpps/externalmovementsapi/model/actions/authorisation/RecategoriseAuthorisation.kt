package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceAuthorisationRecategorised
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.AbsenceCategorisationAction

data class RecategoriseAuthorisation(
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
) : AuthorisationAction,
  AbsenceCategorisationAction {
  constructor(aca: AbsenceCategorisationAction) : this(
    aca.absenceTypeCode,
    aca.absenceSubTypeCode,
    aca.absenceReasonCategoryCode,
    aca.absenceReasonCode,
    aca.reasonPath,
    aca.reason,
  )

  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationRecategorised(taa.person.identifier, taa.id)
}
