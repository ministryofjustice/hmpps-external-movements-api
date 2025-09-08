package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import uk.gov.justice.digital.hmpps.externalmovementsapi.entity.ReferenceDataDomain

data class AbsenceCategorisations(val domain: CodedDescription, val items: List<AbsenceCategorisation>)
data class AbsenceCategorisation(
  val code: String,
  val description: String,
  val hintText: String?,
  val nextDomain: ReferenceDataDomain.Type?,
)
