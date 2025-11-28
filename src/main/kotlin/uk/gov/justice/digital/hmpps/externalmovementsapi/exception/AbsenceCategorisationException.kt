package uk.gov.justice.digital.hmpps.externalmovementsapi.exception

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData

data class AbsenceCategorisationException(val previous: ReferenceData, val optionCount: Int) : RuntimeException("Invalid absence categorisation provided")
