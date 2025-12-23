package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain

interface DomainLinkedReferenceData : ReferenceData {
  val nextDomain: ReferenceDataDomain.Code?
}
