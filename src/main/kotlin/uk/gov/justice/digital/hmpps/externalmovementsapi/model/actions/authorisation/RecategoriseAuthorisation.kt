package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.ReasonPath
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRequired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
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
  AbsenceCategorisationAction,
  ReferenceDataRequired {
  constructor(aca: AbsenceCategorisationAction) : this(
    aca.absenceTypeCode,
    aca.absenceSubTypeCode,
    aca.absenceReasonCategoryCode,
    aca.absenceReasonCode,
    aca.reasonPath,
    aca.reason,
  )

  override fun domainEvent(taa: TemporaryAbsenceAuthorisation): DomainEvent<*> = TemporaryAbsenceAuthorisationRecategorised(taa.person.identifier, taa.id)

  override fun requiredReferenceData(): Set<ReferenceDataKey> = setOfNotNull(
    absenceTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_TYPE of it },
    absenceSubTypeCode?.let { ReferenceDataDomain.Code.ABSENCE_SUB_TYPE of it },
    absenceReasonCategoryCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON_CATEGORY of it },
    absenceReasonCode?.let { ReferenceDataDomain.Code.ABSENCE_REASON of it },
  )
}
