package uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain

data class AbsenceCategorisations(val domain: CodedDescription, val items: List<AbsenceCategorisation>)
data class AbsenceCategorisation(
  val code: String,
  val description: String,
  val hintText: String?,
  val nextDomain: ReferenceDataDomain.Code?,
)

data class PrisonAbsenceCategorisations(
  val types: List<CodedDescription>,
  val subTypes: List<CodedDescription>,
  val reasonCategories: List<CodedDescription>,
  val reasons: List<CodedDescription>,
)
