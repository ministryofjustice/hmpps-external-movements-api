package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

fun interface ReferenceDataRequired {
  fun requiredReferenceData(): Set<ReferenceDataKey>
}

interface ReferenceDataLookup {
  val domain: ReferenceDataDomain.Code
  val code: String
}

data class ReferenceDataKey(
  override val domain: ReferenceDataDomain.Code,
  override val code: String,
) : ReferenceDataLookup

infix fun ReferenceDataDomain.Code.of(code: String): ReferenceDataKey = ReferenceDataKey(this, code)
