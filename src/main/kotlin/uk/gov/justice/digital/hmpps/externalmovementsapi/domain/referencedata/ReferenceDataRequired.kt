package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

fun interface ReferenceDataRequired {
  fun requiredReferenceData(): Set<ReferenceDataKey>
}
