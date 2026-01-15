package uk.gov.justice.digital.hmpps.externalmovementsapi.model.paged

import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain
import java.util.SequencedSet

data class AbsenceCategorisationFilter(
  val domainCode: ReferenceDataDomain.Code,
  @NotEmpty
  val codes: SequencedSet<String>,
)
