package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata.CodedDescription

data class AbsenceCategorisations(val domain: CodedDescription, val items: List<AbsenceCategorisation>)
data class AbsenceCategorisation(
  val code: String,
  val description: String,
  val hintText: String?,
  val nextDomain: ReferenceDataDomain.Code?,
)
