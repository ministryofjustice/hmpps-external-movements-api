package uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Hintable

data class CodedDescription(val code: String, val description: String, val hintText: String? = null)

fun ReferenceData.asCodedDescription(): CodedDescription = CodedDescription(code, description, if (this is Hintable) hintText else null)
